package busy_bees.gametest;

import busy_bees.BusyBeeSupport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchflowerCropBlock;

public class NightTargetGameTest {

	@GameTest(maxTicks = 40)
	public void nightTargetsOnlyQualifyingTorchflowers(GameTestHelper context) {
		context.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		var level = context.getLevel();
		level.setRainLevel(0.0F);
		context.assertTrue(
				BusyBeeSupport.attractsBee(level, Blocks.TORCHFLOWER.defaultBlockState(), true, true),
				"torchflower should attract at night"
		);
		context.assertTrue(
				BusyBeeSupport.attractsBee(
						level,
						Blocks.TORCHFLOWER_CROP.defaultBlockState().setValue(TorchflowerCropBlock.AGE, 1),
						false,
						true
				),
				"fully grown crop should attract at night even if vanilla ignores the crop"
		);
		context.assertFalse(
				BusyBeeSupport.attractsBee(level, Blocks.DANDELION.defaultBlockState(), true, true),
				"dandelion should not attract at night"
		);
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void daytimeKeepsVanillaFlowerTargets(GameTestHelper context) {
		context.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		var level = context.getLevel();
		level.setRainLevel(0.0F);
		boolean dandelionVanilla = Bee.attractsBees(Blocks.DANDELION.defaultBlockState());
		context.assertTrue(dandelionVanilla, "dandelion is a vanilla bee flower");
		context.assertTrue(
				BusyBeeSupport.attractsBee(level, Blocks.DANDELION.defaultBlockState(), dandelionVanilla, false),
				"daytime should still attract dandelions"
		);
		context.succeed();
	}
}
