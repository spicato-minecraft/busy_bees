package busy_bees.gametest;

import busy_bees.BusyBeeData;
import busy_bees.BusyBeeSupport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class PitcherPollinateSetsFlag {

	private static final String POLLINATE_GOAL = "net.minecraft.world.entity.animal.bee.Bee$BeePollinateGoal";
	/** Vanilla grants nectar from {@code stop} only after more than 400 successful pollinating ticks. */
	private static final int POLLINATE_UNTIL_NECTAR = 401;

	@GameTest(maxTicks = 40)
	public void pitcherPollinateSetsFlag(GameTestHelper context) {
		BlockPos flowerRel = new BlockPos(2, 1, 2);
		context.setBlock(flowerRel, Blocks.PITCHER_PLANT);
		BlockPos flower = context.absolutePos(flowerRel);
		context.assertTrue(
				BusyBeeSupport.isMaturePitcherPlant(context.getLevel().getBlockState(flower)),
				"the flower should be a mature pitcher plant"
		);

		Bee bee = context.spawn(EntityTypes.BEE, flowerRel.getX(), flowerRel.getY() + 1, flowerRel.getZ(), EntitySpawnReason.NATURAL);
		bee.setAge(0);
		context.getLevel().setRainLevel(0.0F);
		BusyBeeData data = (BusyBeeData) bee;
		context.assertFalse(data.busyBees$hasPitcherPollen(), "pitcher pollen should start unset");
		context.assertFalse(bee.hasNectar(), "the bee should still be looking for nectar");

		Goal pollinate = pollinateGoal(bee);
		context.assertTrue(pollinate != null, "bee should have a pollinate goal");
		bee.setSavedFlowerPos(flower);
		Vec3 hover = Vec3.atBottomCenterOf(flower).add(0.0, 0.6, 0.0);
		bee.setPos(hover.x, hover.y, hover.z);
		pollinate.start();
		for (int i = 0; i < POLLINATE_UNTIL_NECTAR; i++) {
			pollinate.tick();
		}
		pollinate.stop();

		context.assertTrue(bee.hasNectar(), "finishing pollination should grant nectar");
		context.assertTrue(
				data.busyBees$hasPitcherPollen(),
				"nectar from a mature pitcher plant should set pitcher pollen"
		);
		context.succeed();
	}

	private static Goal pollinateGoal(Bee bee) {
		for (WrappedGoal wrapped : bee.getGoalSelector().getAvailableGoals()) {
			Goal goal = wrapped.getGoal();
			if (POLLINATE_GOAL.equals(goal.getClass().getName())) {
				return goal;
			}
		}
		return null;
	}
}
