package busy_bees.gametest;

import busy_bees.BusyBeeData;
import busy_bees.BusyBeeSupport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public class FlyoverBoneMealsCrop {

	@GameTest(maxTicks = 40)
	public void flyoverBoneMealsCrop(GameTestHelper context) {
		BlockPos farmlandRel = new BlockPos(2, 1, 2);
		BlockPos bareRel = new BlockPos(2, 1, 5);
		context.setBlock(farmlandRel, Blocks.FARMLAND);
		context.setBlock(farmlandRel.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0));
		context.setBlock(bareRel, Blocks.FARMLAND);
		for (int i = 1; i <= 6; i++) {
			context.setBlock(farmlandRel.above(i + 1), Blocks.AIR);
			context.setBlock(bareRel.above(i), Blocks.AIR);
		}

		ServerLevel level = context.getLevel();
		BlockPos wheat = context.absolutePos(farmlandRel.above());
		BlockPos bare = context.absolutePos(bareRel);
		Bee bee = context.spawnWithNoFreeWill(EntityTypes.BEE, farmlandRel.getX(), farmlandRel.getY() + 4, farmlandRel.getZ());
		giveNectar(level, bee);
		((BusyBeeData) bee).busyBees$setPitcherPollen(true);
		bee.setNoGravity(true);
		bee.setNoAi(true);
		context.assertTrue(bee.hasNectar(), "flyover pollen is nectar the bee is carrying");
		context.assertTrue(((BusyBeeData) bee).busyBees$hasPitcherPollen(), "bee should be carrying pitcher pollen");

		hoverAbove(bee, wheat);
		alignNextServerTick(bee);
		int ageBefore = level.getBlockState(wheat).getValue(CropBlock.AGE);
		level.tickNonPassenger(bee);
		BlockState grown = level.getBlockState(wheat);
		context.assertTrue(grown.is(Blocks.WHEAT), "the crop should still be wheat after flyover");
		context.assertTrue(
				grown.getValue(CropBlock.AGE) > ageBefore,
				"young crop should be older after the bone meal tick, age " + grown.getValue(CropBlock.AGE)
		);

		BlockState bareBefore = level.getBlockState(bare);
		context.assertTrue(BusyBeeSupport.isBareFarmland(bareBefore), "the second column should be bare farmland");
		hoverAbove(bee, bare);
		bee.setDeltaMovement(Vec3.ZERO);
		alignNextServerTick(bee);
		level.tickNonPassenger(bee);
		context.assertTrue(
				Math.abs(bee.getX() - (bare.getX() + 0.5)) < 0.75 && Math.abs(bee.getZ() - (bare.getZ() + 0.5)) < 0.75,
				"bee should still be above the bare farmland when flyover runs, at " + bee.position()
		);
		context.assertTrue(
				level.getBlockState(bare).equals(bareBefore),
				"bare farmland should not be bone-mealed"
		);
		context.assertTrue(level.getBlockState(bare.above()).isAir(), "bare farmland should not grow a crop");
		context.succeed();
	}

	private static void hoverAbove(Bee bee, BlockPos block) {
		bee.setPos(block.getX() + 0.5, block.getY() + 4.0, block.getZ() + 0.5);
		bee.setDeltaMovement(Vec3.ZERO);
	}

	/**
	 * {@code tickNonPassenger} increments tickCount before {@code Bee.tick}, where flyover runs.
	 */
	private static void alignNextServerTick(Bee bee) {
		int id = bee.getId();
		bee.tickCount = Math.floorMod(-(id + 1), BusyBeeSupport.FLYOVER_INTERVAL_TICKS);
	}

	private static void giveNectar(ServerLevel level, Bee bee) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		bee.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		tag.putBoolean(Bee.TAG_HAS_NECTAR, true);
		bee.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
	}
}
