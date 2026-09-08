package busy_bees;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class MinecraftTestBootstrap {

	private static boolean initialized;

	private MinecraftTestBootstrap() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		initialized = true;
	}
}
