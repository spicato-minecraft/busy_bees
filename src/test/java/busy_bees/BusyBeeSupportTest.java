package busy_bees;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusyBeeSupportTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void modMainClassLoads() {
		assertNotNull(BusyBees.MOD_ID);
	}

	@Test
	void matureFlowerAndGrownCropQualify() {
		assertTrue(BusyBeeSupport.isQualifyingTorchflower(Blocks.TORCHFLOWER.defaultBlockState()));
		assertTrue(BusyBeeSupport.isQualifyingTorchflower(
				Blocks.TORCHFLOWER_CROP.defaultBlockState().setValue(TorchflowerCropBlock.AGE, 1)
		));
	}

	@Test
	void immatureCropPotAndOtherFlowersDoNotQualify() {
		assertFalse(BusyBeeSupport.isQualifyingTorchflower(
				Blocks.TORCHFLOWER_CROP.defaultBlockState().setValue(TorchflowerCropBlock.AGE, 0)
		));
		assertFalse(BusyBeeSupport.isQualifyingTorchflower(Blocks.POTTED_TORCHFLOWER.defaultBlockState()));
		assertFalse(BusyBeeSupport.isQualifyingTorchflower(Blocks.DANDELION.defaultBlockState()));
	}

	@Test
	void maturePitcherPlantQualifiesButCropDoesNot() {
		assertTrue(BusyBeeSupport.isMaturePitcherPlant(Blocks.PITCHER_PLANT.defaultBlockState()));
		assertFalse(BusyBeeSupport.isMaturePitcherPlant(
				Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE, PitcherCropBlock.MAX_AGE)
		));
		assertFalse(BusyBeeSupport.isMaturePitcherPlant(Blocks.TORCHFLOWER.defaultBlockState()));
	}

	@Test
	void pitcherGrowDelayIsFasterThanVanilla() {
		assertEquals(30, BusyBeeSupport.growCropDelay(30, false));
		assertTrue(BusyBeeSupport.growCropDelay(30, true) < 30);
		assertEquals(Math.round(30 / 1.3F), BusyBeeSupport.growCropDelay(30, true));
	}

	@Test
	void foundingBandExcludesInnerHiveRange() {
		BlockPos origin = BlockPos.ZERO;
		assertFalse(BusyBeeSupport.isInFoundingBand(origin, new BlockPos(10, 0, 0)));
		assertTrue(BusyBeeSupport.isInFoundingBand(origin, new BlockPos(22, 0, 0)));
		assertTrue(BusyBeeSupport.isInFoundingBand(origin, new BlockPos(40, 0, 0)));
		assertFalse(BusyBeeSupport.isInFoundingBand(origin, new BlockPos(41, 0, 0)));
	}

	@Test
	void farmlandAndCropsAreExcludedFromFlyover() {
		assertTrue(BusyBeeSupport.isFarmlandOrCrop(Blocks.FARMLAND.defaultBlockState()));
		assertTrue(BusyBeeSupport.isFarmlandOrCrop(Blocks.WHEAT.defaultBlockState()));
		assertFalse(BusyBeeSupport.isFarmlandOrCrop(Blocks.GRASS_BLOCK.defaultBlockState()));
	}

	@Test
	void hiveAndNestAreDetected() {
		assertTrue(BusyBeeSupport.isHiveOrNest(Blocks.BEEHIVE.defaultBlockState()));
		assertTrue(BusyBeeSupport.isHiveOrNest(Blocks.BEE_NEST.defaultBlockState()));
		assertFalse(BusyBeeSupport.isHiveOrNest(Blocks.OAK_LOG.defaultBlockState()));
	}
}
