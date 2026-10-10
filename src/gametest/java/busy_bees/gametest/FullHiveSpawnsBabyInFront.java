package busy_bees.gametest;

import busy_bees.BusyBeeHiveFeeding;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class FullHiveSpawnsBabyInFront {

	@GameTest(maxTicks = 40)
	public void fullHiveSpawnsBabyInFront(GameTestHelper context) {
		BlockPos hiveRel = new BlockPos(2, 1, 2);
		Direction facing = Direction.SOUTH;
		context.setBlock(hiveRel, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.FACING, facing));
		context.setBlock(hiveRel.relative(facing), Blocks.AIR);
		BeehiveBlockEntity hive = context.getBlockEntity(hiveRel, BeehiveBlockEntity.class);
		context.assertTrue(hive != null, "hive block entity should exist");
		hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
		hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
		hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
		context.assertTrue(hive.isFull(), "three adults should fill the hive");
		context.assertTrue(
				hive.getOccupantCount() == BeehiveBlockEntity.MAX_OCCUPANTS,
				"hive should start at occupant capacity"
		);

		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		ItemStack flower = new ItemStack(Items.DANDELION, 8);
		BlockPos hivePos = context.absolutePos(hiveRel);
		context.assertTrue(
				BusyBeeHiveFeeding.tryUseFlowerOnHive(context.getLevel(), hivePos, player, flower),
				"first flower should love one stored adult"
		);
		context.assertTrue(
				hive.getOccupantCount() == BeehiveBlockEntity.MAX_OCCUPANTS,
				"loving an adult should leave the hive full"
		);
		context.assertTrue(babiesInFront(context, hivePos, facing).isEmpty(), "the first flower should not spill a baby");

		context.assertTrue(
				BusyBeeHiveFeeding.tryUseFlowerOnHive(context.getLevel(), hivePos, player, flower),
				"second flower should breed the stored adults"
		);
		context.assertTrue(
				hive.getOccupantCount() == BeehiveBlockEntity.MAX_OCCUPANTS,
				"stored occupants should stay at capacity when the baby cannot fit"
		);
		context.assertTrue(hive.isFull(), "the hive should still be full");

		BlockPos front = hivePos.relative(facing);
		List<Bee> babies = babiesInFront(context, hivePos, facing);
		context.assertTrue(babies.size() == 1, "one baby should spawn outside a full hive, found " + babies.size());
		Bee baby = babies.getFirst();
		context.assertTrue(baby.isBaby(), "the bee in front of the hive should be a baby");
		context.assertTrue(
				front.equals(baby.blockPosition()),
				"baby should stand in front of the hive, at " + baby.blockPosition() + " front " + front
		);
		context.succeed();
	}

	private static List<Bee> babiesInFront(GameTestHelper context, BlockPos hivePos, Direction facing) {
		BlockPos front = hivePos.relative(facing);
		AABB box = new AABB(front).inflate(0.5);
		List<Bee> babies = new ArrayList<>();
		for (Bee bee : context.getLevel().getEntitiesOfClass(Bee.class, box)) {
			if (bee.isBaby()) {
				babies.add(bee);
			}
		}
		return babies;
	}
}
