package busy_bees.gametest;

import busy_bees.BusyBees;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.LenientJsonParser;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads every common mixin target so a renamed class fails here instead of in a player session.
 * {@code @Mixin} is not retained at runtime, so each mixin class file is read for its target names.
 */
public class ServerMixinApplySweep {

	private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

	@GameTest(maxTicks = 20)
	public void loadsMixinTargets(GameTestHelper context) {
		ClassLoader loader = BusyBees.class.getClassLoader();
		JsonObject root = readMixinConfig(context, loader);
		String pkg = root.get("package").getAsString();
		Set<String> loaded = new LinkedHashSet<>();
		for (JsonElement element : root.getAsJsonArray("mixins")) {
			String mixinName = pkg + "." + element.getAsString();
			MixinClass mixin = readMixinClass(context, loader, mixinName);
			context.assertTrue(mixin.declaredMixin(), mixinName + " should declare @Mixin");
			context.assertTrue(!mixin.targets().isEmpty(), mixinName + " should name a target class");
			for (String binaryName : mixin.targets()) {
				if (!mixin.behavior() && loaded.contains(binaryName)) {
					continue;
				}
				loadTarget(context, loader, binaryName);
				loaded.add(binaryName);
			}
		}
		context.assertTrue(!loaded.isEmpty(), "mixin config should name target classes");
		context.succeed();
	}

	private static JsonObject readMixinConfig(GameTestHelper context, ClassLoader loader) {
		try (InputStream in = loader.getResourceAsStream("busy_bees.mixins.json")) {
			context.assertTrue(in != null, "busy_bees.mixins.json should be on the classpath");
			String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return LenientJsonParser.parse(json).getAsJsonObject();
		} catch (IOException e) {
			throw context.assertionException("Could not read busy_bees.mixins.json");
		}
	}

	private static MixinClass readMixinClass(GameTestHelper context, ClassLoader loader, String mixinName) {
		String resource = mixinName.replace('.', '/') + ".class";
		try (InputStream in = loader.getResourceAsStream(resource)) {
			context.assertTrue(in != null, "mixin class should be on the classpath: " + mixinName);
			return MixinClass.parse(in.readAllBytes());
		} catch (IOException e) {
			throw context.assertionException("Could not read mixin class " + mixinName);
		}
	}

	private static void loadTarget(GameTestHelper context, ClassLoader loader, String binaryName) {
		try {
			Class.forName(binaryName, true, loader);
		} catch (ClassNotFoundException e) {
			throw context.assertionException("Mixin target failed to load: " + binaryName);
		}
	}

	private record MixinClass(boolean behavior, boolean declaredMixin, List<String> targets) {
		private static MixinClass parse(byte[] bytes) throws IOException {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
			if (in.readInt() != 0xCAFEBABE) {
				throw new IOException("not a class file");
			}
			in.readUnsignedShort();
			in.readUnsignedShort();
			String[] utf8 = readPool(in);
			boolean isInterface = (in.readUnsignedShort() & 0x0200) != 0;
			in.readUnsignedShort();
			in.readUnsignedShort();
			int interfaces = in.readUnsignedShort();
			for (int i = 0; i < interfaces; i++) {
				in.readUnsignedShort();
			}
			skipMembers(in, utf8, false);
			boolean methodsAreBehavior = skipMembers(in, utf8, true);
			boolean behavior = !isInterface && methodsAreBehavior;
			boolean declaredMixin = false;
			List<String> targets = new ArrayList<>();
			for (Attribute attribute : readAttributes(in, utf8)) {
				if (!isAnnotationAttribute(attribute.name())) {
					continue;
				}
				declaredMixin |= readMixinTargets(attribute.data(), utf8, targets);
			}
			return new MixinClass(behavior, declaredMixin, List.copyOf(targets));
		}

		private static String[] readPool(DataInputStream in) throws IOException {
			int count = in.readUnsignedShort();
			String[] utf8 = new String[count];
			for (int i = 1; i < count; i++) {
				int tag = in.readUnsignedByte();
				switch (tag) {
					case 1 -> utf8[i] = in.readUTF();
					case 7, 8, 16, 19, 20 -> in.readUnsignedShort();
					case 15 -> {
						in.readUnsignedByte();
						in.readUnsignedShort();
					}
					case 3, 4 -> in.readInt();
					case 9, 10, 11, 12, 17, 18 -> {
						in.readUnsignedShort();
						in.readUnsignedShort();
					}
					case 5, 6 -> {
						in.readLong();
						i++;
					}
					default -> throw new IOException("unknown constant pool tag " + tag);
				}
			}
			return utf8;
		}

		private static boolean skipMembers(DataInputStream in, String[] utf8, boolean methods) throws IOException {
			int count = in.readUnsignedShort();
			boolean behavior = false;
			for (int i = 0; i < count; i++) {
				in.readUnsignedShort();
				in.readUnsignedShort();
				in.readUnsignedShort();
				for (Attribute attribute : readAttributes(in, utf8)) {
					if (methods && isAnnotationAttribute(attribute.name()) && hasInjector(attribute.data(), utf8)) {
						behavior = true;
					}
				}
			}
			return behavior;
		}

		private static List<Attribute> readAttributes(DataInputStream in, String[] utf8) throws IOException {
			int count = in.readUnsignedShort();
			List<Attribute> attributes = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				String name = utf8[in.readUnsignedShort()];
				int length = in.readInt();
				attributes.add(new Attribute(name, in.readNBytes(length)));
			}
			return attributes;
		}

		private static boolean isAnnotationAttribute(String name) {
			return "RuntimeVisibleAnnotations".equals(name) || "RuntimeInvisibleAnnotations".equals(name);
		}

		private static boolean hasInjector(byte[] data, String[] utf8) throws IOException {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			int annotations = in.readUnsignedShort();
			for (int i = 0; i < annotations; i++) {
				String descriptor = utf8[in.readUnsignedShort()];
				if (descriptor.contains("mixin/injection/")
						|| descriptor.contains("mixinextras/")
						|| descriptor.endsWith("Overwrite;")) {
					return true;
				}
				skipAnnotationValues(in);
			}
			return false;
		}

		private static boolean readMixinTargets(byte[] data, String[] utf8, List<String> targets) throws IOException {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			int annotations = in.readUnsignedShort();
			boolean declared = false;
			for (int i = 0; i < annotations; i++) {
				String descriptor = utf8[in.readUnsignedShort()];
				int pairs = in.readUnsignedShort();
				if (!MIXIN_ANNOTATION.equals(descriptor)) {
					for (int pair = 0; pair < pairs; pair++) {
						in.readUnsignedShort();
						skipElement(in);
					}
					continue;
				}
				declared = true;
				for (int pair = 0; pair < pairs; pair++) {
					String name = utf8[in.readUnsignedShort()];
					if ("value".equals(name) || "targets".equals(name)) {
						targets.addAll(readTargetNames(in, utf8));
					} else {
						skipElement(in);
					}
				}
			}
			return declared;
		}

		private static List<String> readTargetNames(DataInputStream in, String[] utf8) throws IOException {
			int tag = in.readUnsignedByte();
			if (tag == '[') {
				int count = in.readUnsignedShort();
				List<String> names = new ArrayList<>();
				for (int i = 0; i < count; i++) {
					int element = in.readUnsignedByte();
					String value = utf8[in.readUnsignedShort()];
					if (element == 'c') {
						names.add(descriptorToBinary(value));
					} else if (element == 's') {
						names.add(value);
					}
				}
				return names;
			}
			if (tag == 'c') {
				return List.of(descriptorToBinary(utf8[in.readUnsignedShort()]));
			}
			if (tag == 's') {
				return List.of(utf8[in.readUnsignedShort()]);
			}
			skipElementRest(in, tag);
			return List.of();
		}

		private static String descriptorToBinary(String descriptor) {
			String body = descriptor;
			if (body.startsWith("L") && body.endsWith(";")) {
				body = body.substring(1, body.length() - 1);
			}
			return body.replace('/', '.');
		}

		private static void skipAnnotationValues(DataInputStream in) throws IOException {
			int pairs = in.readUnsignedShort();
			for (int i = 0; i < pairs; i++) {
				in.readUnsignedShort();
				skipElement(in);
			}
		}

		private static void skipElement(DataInputStream in) throws IOException {
			skipElementRest(in, in.readUnsignedByte());
		}

		private static void skipElementRest(DataInputStream in, int tag) throws IOException {
			switch (tag) {
				case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 's', 'c' -> in.readUnsignedShort();
				case 'e' -> {
					in.readUnsignedShort();
					in.readUnsignedShort();
				}
				case '@' -> {
					in.readUnsignedShort();
					skipAnnotationValues(in);
				}
				case '[' -> {
					int count = in.readUnsignedShort();
					for (int i = 0; i < count; i++) {
						skipElement(in);
					}
				}
				default -> throw new IOException("bad annotation element " + tag);
			}
		}
	}

	private record Attribute(String name, byte[] data) {
	}
}
