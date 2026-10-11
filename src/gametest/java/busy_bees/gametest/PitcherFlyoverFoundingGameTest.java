package busy_bees.gametest;

import busy_bees.BusyBeeData;
import busy_bees.BusyBeeHiveFeeding;
import busy_bees.BusyBeeSupport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

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
		boolean foundNest = BusyBeeSupport.findClosestMatch(log, 3, 2, pos -> context.getLevel().getBlockState(pos).is(Blocks.BEE_NEST)).isPresent();
		context.assertTrue(foundNest, "nest block should exist next to the log");
		context.succeed();
	}

	@GameTest(maxTicks = 80)
	public void foundingDwellPlacesNestAndBeeEnters(GameTestHelper context) {
		BlockPos originRel = new BlockPos(1, 1, 1);
		context.setBlock(originRel, Blocks.BEEHIVE);
		BlockPos origin = context.absolutePos(originRel);
		BlockPos log = origin.offset(24, 0, 0);
		context.getLevel().setBlockAndUpdate(log, Blocks.OAK_LOG.defaultBlockState());
		BusyBeeSupport.FoundingSite site = BusyBeeSupport.findFoundingSite(
				context.getLevel(),
				origin,
				log,
				context.getLevel().getRandom()
		);
		context.assertTrue(site != null, "log in band should yield a nest site");
		Bee bee = context.spawn(EntityTypes.BEE, 1, 2, 1, EntitySpawnReason.NATURAL);
		bee.setAge(0);
		bee.setPos(site.nestPos().getX() + 0.5, site.nestPos().getY(), site.nestPos().getZ() + 0.5);
		BusyBeeData data = (BusyBeeData) bee;
		data.busyBees$beginFounding(origin);
		data.busyBees$setFoundingTarget(site.nestPos());
		data.busyBees$setFoundingFacing(site.facing());
		data.busyBees$setFoundingDwell(BusyBeeSupport.FOUNDING_DWELL_TICKS - 1);
		BusyBeeSupport.tickFounding(context.getLevel(), bee, data);
		context.assertTrue(
				context.getLevel().getBlockState(site.nestPos()).is(Blocks.BEE_NEST),
				"nest should appear after the bee dwells at the site"
		);
		BeehiveBlockEntity nest = context.getLevel().getBlockEntity(site.nestPos(), BlockEntityTypes.BEEHIVE).orElse(null);
		context.assertTrue(nest != null && nest.getOccupantCount() == 1, "founding bee should enter the new nest");
		context.assertTrue(bee.isRemoved(), "bee entity should occupy the nest");
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void flyoverFindsGrassFarBelowTheBee(GameTestHelper context) {
		var level = context.getLevel();
		BlockPos grass = context.absolutePos(new BlockPos(2, 1, 2)).offset(4, 8, 4);
		level.setBlockAndUpdate(grass, Blocks.GRASS_BLOCK.defaultBlockState());
		for (int i = 1; i <= 20; i++) {
			level.setBlockAndUpdate(grass.above(i), Blocks.AIR.defaultBlockState());
		}
		BlockPos found = BusyBeeSupport.findFlyoverTarget(level, grass.above(20));
		context.assertTrue(grass.equals(found), "grass 20 blocks below bee feet should be the flyover target, found " + found);
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void wheatIsAFlyoverPollenTarget(GameTestHelper context) {
		var level = context.getLevel();
		BlockPos farmland = context.absolutePos(new BlockPos(2, 1, 2)).offset(4, 2, 4);
		level.setBlockAndUpdate(farmland, Blocks.FARMLAND.defaultBlockState());
		BlockPos wheat = farmland.above();
		level.setBlockAndUpdate(wheat, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0));
		for (int i = 1; i <= 6; i++) {
			level.setBlockAndUpdate(wheat.above(i), Blocks.AIR.defaultBlockState());
		}
		BlockPos found = BusyBeeSupport.findFlyoverTarget(level, wheat.above(6));
		context.assertTrue(wheat.equals(found), "wheat under the bee should take flyover pollen, found " + found);
		context.assertTrue(
				BusyBeeSupport.isFlyoverPollenTarget(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0)),
				"wheat is a flyover pollen target"
		);
		context.assertFalse(
				BusyBeeSupport.isFlyoverPollenTarget(Blocks.FARMLAND.defaultBlockState()),
				"bare farmland is not a flyover pollen target"
		);
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void hiveWithForagerStillCountsAsRegisteredFull(GameTestHelper context) {
		BlockPos originRel = new BlockPos(2, 1, 2);
		BlockPos destRel = new BlockPos(2, 1, 5);
		context.setBlock(originRel, Blocks.BEEHIVE);
		context.setBlock(destRel, Blocks.BEE_NEST);
		BeehiveBlockEntity dest = context.getBlockEntity(destRel, BeehiveBlockEntity.class);
		context.assertTrue(dest != null, "destination nest should exist");
		dest.storeBee(BeehiveBlockEntity.Occupant.create(0));
		dest.storeBee(BeehiveBlockEntity.Occupant.create(0));
		Bee forager = context.spawn(EntityTypes.BEE, destRel.getX(), destRel.getY() + 1, destRel.getZ(), EntitySpawnReason.NATURAL);
		forager.setHivePos(context.absolutePos(destRel));
		Bee overflow = context.spawn(EntityTypes.BEE, originRel.getX(), originRel.getY() + 1, originRel.getZ(), EntitySpawnReason.NATURAL);
		overflow.setAge(0);
		BlockPos destAbs = context.absolutePos(destRel);
		context.assertFalse(
				BusyBeeSupport.hasRegisteredVacancy(context.getLevel(), destAbs, overflow),
				"two stored bees plus a forager assigned to the nest should fill it"
		);
		context.assertTrue(
				BusyBeeSupport.findHiveWithRegisteredVacancy(
						context.getLevel(),
						context.absolutePos(originRel),
						overflow,
						3,
						2
				) == null,
				"overflow must not steal a hive whose three bees are merely out collecting"
		);
		context.succeed();
	}

	@GameTest(maxTicks = 80)
	public void overflowFliesStraightToARegisteredVacancy(GameTestHelper context) {
		BlockPos originRel = new BlockPos(2, 1, 2);
		BlockPos destRel = new BlockPos(2, 1, 5);
		context.setBlock(originRel, Blocks.BEEHIVE);
		context.setBlock(destRel, Blocks.BEE_NEST);
		BeehiveBlockEntity origin = context.getBlockEntity(originRel, BeehiveBlockEntity.class);
		BeehiveBlockEntity dest = context.getBlockEntity(destRel, BeehiveBlockEntity.class);
		context.assertTrue(origin != null && dest != null, "both hives should exist");
		origin.storeBee(BeehiveBlockEntity.Occupant.create(0));
		origin.storeBee(BeehiveBlockEntity.Occupant.create(0));
		origin.storeBee(BeehiveBlockEntity.Occupant.create(0));
		Bee overflow = context.spawn(EntityTypes.BEE, originRel.getX(), originRel.getY() + 1, originRel.getZ(), EntitySpawnReason.NATURAL);
		overflow.setAge(0);
		overflow.setHivePos(context.absolutePos(originRel));
		BlockPos destAbs = context.absolutePos(destRel);
		BlockPos found = BusyBeeSupport.findHiveWithRegisteredVacancy(
				context.getLevel(),
				context.absolutePos(originRel),
				overflow,
				4,
				2
		);
		context.assertTrue(destAbs.equals(found), "empty nest should be the relocate target, found " + found);
		BusyBeeData data = (BusyBeeData) overflow;
		data.busyBees$beginRelocate(context.absolutePos(originRel), destAbs);
		overflow.setHivePos(destAbs);
		context.assertTrue(data.busyBees$isRelocating(), "overflow bee should relocate, not wander");
		context.assertFalse(data.busyBees$isFounding(), "a vacancy must skip founding");
		BusyBeeSupport.tickRelocate(context.getLevel(), overflow, data);
		context.assertTrue(
				overflow.getNavigation().isInProgress() || overflow.blockPosition().closerThan(destAbs, 2),
				"relocating bee should path straight at the vacant hive"
		);
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void overflowFoundsWhenNearbyHivesAreRegisteredFull(GameTestHelper context) {
		BlockPos originRel = new BlockPos(2, 1, 2);
		BlockPos destRel = new BlockPos(2, 1, 5);
		context.setBlock(originRel, Blocks.BEEHIVE);
		context.setBlock(destRel, Blocks.BEE_NEST);
		BeehiveBlockEntity origin = context.getBlockEntity(originRel, BeehiveBlockEntity.class);
		BeehiveBlockEntity dest = context.getBlockEntity(destRel, BeehiveBlockEntity.class);
		context.assertTrue(origin != null && dest != null, "both hives should exist");
		origin.storeBee(BeehiveBlockEntity.Occupant.create(0));
		origin.storeBee(BeehiveBlockEntity.Occupant.create(0));
		origin.storeBee(BeehiveBlockEntity.Occupant.create(0));
		dest.storeBee(BeehiveBlockEntity.Occupant.create(0));
		dest.storeBee(BeehiveBlockEntity.Occupant.create(0));
		dest.storeBee(BeehiveBlockEntity.Occupant.create(0));
		Bee overflow = context.spawn(EntityTypes.BEE, originRel.getX(), originRel.getY() + 1, originRel.getZ(), EntitySpawnReason.NATURAL);
		overflow.setAge(0);
		context.assertTrue(
				BusyBeeSupport.canFoundNest(overflow)
						&& BusyBeeSupport.isHiveFull(origin)
						&& BusyBeeSupport.findHiveWithRegisteredVacancy(
								context.getLevel(),
								context.absolutePos(originRel),
								overflow,
								3,
								2
						) == null,
				"founding is the fallback when every nearby hive already has three registered bees"
		);
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

	@GameTest(maxTicks = 40)
	public void hiveFlowersBreedStoredAdults(GameTestHelper context) {
		BlockPos hiveRel = new BlockPos(2, 1, 2);
		context.setBlock(hiveRel, Blocks.BEEHIVE);
		BeehiveBlockEntity hive = context.getBlockEntity(hiveRel, BeehiveBlockEntity.class);
		context.assertTrue(hive != null, "hive block entity should exist");
		hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
		hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		ItemStack flower = new ItemStack(Items.DANDELION, 8);
		BlockPos hivePos = context.absolutePos(hiveRel);
		context.assertTrue(
				BusyBeeHiveFeeding.tryUseFlowerOnHive(context.getLevel(), hivePos, player, flower),
				"first flower should love one stored adult"
		);
		context.assertTrue(
				BusyBeeHiveFeeding.tryUseFlowerOnHive(context.getLevel(), hivePos, player, flower),
				"second flower should breed the stored adults"
		);
		context.assertTrue(hive.getOccupantCount() == 3, "baby should occupy the hive when there is room");
		context.assertTrue(
				BusyBeeHiveFeeding.tryUseFlowerOnHive(context.getLevel(), hivePos, player, flower),
				"a further flower should age the stored baby"
		);
		context.assertTrue(hive.getOccupantCount() == 3, "aging the baby must not spawn more bees");
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void enteredAdultKeepsLoveAcrossFeeds(GameTestHelper context) {
		BlockPos hiveRel = new BlockPos(2, 1, 2);
		context.setBlock(hiveRel, Blocks.BEEHIVE);
		BeehiveBlockEntity hive = context.getBlockEntity(hiveRel, BeehiveBlockEntity.class);
		context.assertTrue(hive != null, "hive block entity should exist");
		Bee adult = context.spawn(EntityTypes.BEE, 1, 2, 1, EntitySpawnReason.NATURAL);
		adult.setAge(0);
		hive.addOccupant(adult);
		context.assertTrue(hive.getOccupantCount() == 1, "entered bee should occupy the hive");
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		ItemStack flower = new ItemStack(Items.DANDELION, 8);
		BlockPos hivePos = context.absolutePos(hiveRel);
		context.assertTrue(
				BusyBeeHiveFeeding.tryUseFlowerOnHive(context.getLevel(), hivePos, player, flower),
				"first flower should love the entered adult"
		);
		context.assertFalse(
				BusyBeeHiveFeeding.tryUseFlowerOnHive(context.getLevel(), hivePos, player, flower),
				"the same adult must not love again in this window"
		);
		context.succeed();
	}
}
