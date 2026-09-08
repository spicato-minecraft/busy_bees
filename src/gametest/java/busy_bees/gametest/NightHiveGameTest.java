package busy_bees.gametest;

import busy_bees.BusyBeeSupport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class NightHiveGameTest {

	private static final int LOCAL = 3;
	private static final int LOCAL_Y = 2;

	@GameTest(maxTicks = 40)
	public void nightStayFalseWhenTorchflowerInRange(GameTestHelper context) {
		BlockPos hive = new BlockPos(2, 1, 2);
		context.setBlock(hive, Blocks.BEEHIVE);
		context.setBlock(hive.east(), Blocks.TORCHFLOWER);
		boolean stay = BusyBeeSupport.beesStayInHive(
				context.getLevel(), context.absolutePos(hive), true, LOCAL, LOCAL_Y
		);
		context.assertFalse(stay, "night + torchflower in range should not keep bees in hive");
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void nightStayTrueWhenOnlyDandelion(GameTestHelper context) {
		BlockPos hive = new BlockPos(2, 1, 2);
		context.setBlock(hive, Blocks.BEEHIVE);
		context.setBlock(hive.east(), Blocks.DANDELION);
		boolean stay = BusyBeeSupport.beesStayInHive(
				context.getLevel(), context.absolutePos(hive), true, LOCAL, LOCAL_Y
		);
		context.assertTrue(stay, "night + only other flowers should keep bees in hive");
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void rainKeepsBeesInEvenWithTorchflower(GameTestHelper context) {
		BlockPos hive = new BlockPos(2, 1, 2);
		context.setBlock(hive, Blocks.BEEHIVE);
		context.setBlock(hive.east(), Blocks.TORCHFLOWER);
		ServerLevel level = context.getLevel();
		level.setWeatherParameters(0, 6000, true, false);
		level.setRainLevel(1.0F);
		context.assertTrue(level.isRaining(), "rain should be active");
		boolean stay = BusyBeeSupport.beesStayInHive(level, context.absolutePos(hive), true, LOCAL, LOCAL_Y);
		context.assertTrue(stay, "rain should still send bees home with torchflowers present");
		context.succeed();
	}
}
