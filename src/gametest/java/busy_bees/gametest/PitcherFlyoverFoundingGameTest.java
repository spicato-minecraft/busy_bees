package busy_bees.gametest;

import busy_bees.BusyBeeSupport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

public class PitcherFlyoverFoundingGameTest {

	@GameTest(maxTicks = 40)
	public void otherHiveInRangeBlocksFounding(GameTestHelper context) {
		BlockPos origin = new BlockPos(2, 1, 2);
		context.setBlock(origin, Blocks.BEEHIVE);
		context.setBlock(origin.east(), Blocks.BEE_NEST);
		context.assertTrue(
				BusyBeeSupport.isOtherHiveOrNestInRange(context.getLevel(), context.absolutePos(origin), 3, 2),
				"a second nest next to the hive should skip founding"
		);
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void isolatedHiveHasNoOtherInRange(GameTestHelper context) {
		BlockPos origin = new BlockPos(2, 1, 2);
		context.setBlock(origin, Blocks.BEEHIVE);
		context.assertFalse(
				BusyBeeSupport.isOtherHiveOrNestInRange(context.getLevel(), context.absolutePos(origin), 3, 2),
				"a lone hive should be allowed to found"
		);
		context.succeed();
	}

	@GameTest(maxTicks = 80)
	public void foundingPlacesNestOnLogInBand(GameTestHelper context) {
		BlockPos originRel = new BlockPos(1, 1, 1);
		context.setBlock(originRel, Blocks.BEEHIVE);
		BlockPos origin = context.absolutePos(originRel);
		BlockPos log = origin.offset(24, 0, 0);
		context.getLevel().setBlockAndUpdate(log, Blocks.OAK_LOG.defaultBlockState());
		boolean placed = BusyBeeSupport.tryAttachNestToTree(context.getLevel(), origin, log);
		context.assertTrue(placed, "a log in the founding band should take a nest");
		boolean foundNest = BlockPos.findClosestMatch(log, 3, 2, pos -> context.getLevel().getBlockState(pos).is(Blocks.BEE_NEST)).isPresent();
		context.assertTrue(foundNest, "nest block should exist next to the log");
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void wheatIsFlyoverExcludedAndGrassIsNot(GameTestHelper context) {
		context.assertTrue(BusyBeeSupport.isFarmlandOrCrop(Blocks.FARMLAND.defaultBlockState()), "farmland is flyover-excluded");
		context.assertTrue(BusyBeeSupport.isFarmlandOrCrop(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0)), "wheat is flyover-excluded");
		context.assertFalse(BusyBeeSupport.isFarmlandOrCrop(Blocks.GRASS_BLOCK.defaultBlockState()), "grass is a flyover surface");
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void pitcherPlantIsBonusSource(GameTestHelper context) {
		context.setBlock(new BlockPos(1, 1, 1), Blocks.PITCHER_PLANT);
		context.assertTrue(
				BusyBeeSupport.isMaturePitcherPlant(context.getBlockState(new BlockPos(1, 1, 1))),
				"placed pitcher plant should grant the pollen bonus"
		);
		context.succeed();
	}
}
