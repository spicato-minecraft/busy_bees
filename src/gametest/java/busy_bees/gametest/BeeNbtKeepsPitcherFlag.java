package busy_bees.gametest;

import busy_bees.BusyBeeData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public class BeeNbtKeepsPitcherFlag {

	@GameTest(maxTicks = 40)
	public void beeNbtKeepsPitcherFlag(GameTestHelper context) {
		Bee original = context.spawn(EntityTypes.BEE, 1, 2, 1, EntitySpawnReason.NATURAL);
		BusyBeeData originalData = (BusyBeeData) original;
		originalData.busyBees$setPitcherPollen(true);
		context.assertTrue(originalData.busyBees$hasPitcherPollen(), "pitcher pollen should be set before saving");

		HolderLookup.Provider lookup = context.getLevel().registryAccess();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
		original.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		context.assertTrue(
				tag.getBooleanOr("busy_bees.pitcher", false),
				"saved bee data should include pitcher pollen"
		);
		context.assertTrue(originalData.busyBees$hasPitcherPollen(), "saving should leave pitcher pollen on the original bee");

		Bee loaded = EntityTypes.BEE.create(context.getLevel(), EntitySpawnReason.LOAD);
		context.assertTrue(loaded != null, "a bee should be created to receive the saved data");
		BusyBeeData loadedData = (BusyBeeData) loaded;
		context.assertFalse(loadedData.busyBees$hasPitcherPollen(), "a new bee should not already have pitcher pollen");
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag));
		context.assertTrue(loadedData.busyBees$hasPitcherPollen(), "pitcher pollen should still be set after loading");
		context.succeed();
	}
}
