package busy_bees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public final class BusyBeeSupport {

	public static final int HIVE_NECTAR_RANGE = 22;
	public static final int HIVE_NECTAR_VERTICAL = 8;
	public static final int FOUNDING_MIN = 22;
	public static final int FOUNDING_MAX = 40;
	/** Larger than nectar/founding-min (22) so overflow bees can still see nearby hives. */
	public static final int HIVE_RELOCATE_RANGE = 48;
	public static final int HIVE_RELOCATE_VERTICAL = HIVE_NECTAR_VERTICAL + 8;
	/** Bee must stay this close to the chosen nest air block. */
	public static final int FOUNDING_STAY_RADIUS = 2;
	/** ~30s at 20 tps, counted only while inside {@link #FOUNDING_STAY_RADIUS}. */
	public static final int FOUNDING_DWELL_TICKS = 600;
	public static final float PITCHER_POLLEN_MULTIPLIER = 1.3F;
	/** Vanilla BeeGrowCropGoal uses delay 30; 30/1.3 ≈ 23 for 1.3× attempts. */
	public static final int VANILLA_GROW_CHANCE_DELAY = 30;
	/** ~4s at 20 tps. Staggered by entity id so bees do not all scan the same tick. */
	public static final int FLYOVER_INTERVAL_TICKS = 80;

	private BusyBeeSupport() {
	}

	public static boolean isQualifyingTorchflower(BlockState state) {
		if (state.is(Blocks.TORCHFLOWER)) {
			return true;
		}
		if (state.is(Blocks.TORCHFLOWER_CROP)) {
			return state.getValue(TorchflowerCropBlock.AGE) >= 1;
		}
		return false;
	}

	public static boolean isMaturePitcherPlant(BlockState state) {
		return state.is(Blocks.PITCHER_PLANT);
	}

	public static boolean isWeatherKeepingBeesInside(Level level) {
		return level.isRaining();
	}

	/**
	 * 26.2 {@code BlockPos.findClosestMatch}: closest hit inside a horizontal/vertical box,
	 * ordered by Manhattan distance. 26.3 folded that search into
	 * {@link BlockPos#withinBoxByManhattanDistance}.
	 */
	public static Optional<BlockPos> findClosestMatch(
			BlockPos origin,
			int horizontalRange,
			int verticalRange,
			Predicate<BlockPos> predicate
	) {
		for (BlockPos pos : BlockPos.withinBoxByManhattanDistance(origin, horizontalRange, verticalRange, horizontalRange)) {
			if (predicate.test(pos)) {
				return Optional.of(pos);
			}
		}
		return Optional.empty();
	}

	public static boolean hasQualifyingTorchflowerInRange(Level level, BlockPos origin) {
		return hasQualifyingTorchflowerInRange(level, origin, HIVE_NECTAR_RANGE, HIVE_NECTAR_VERTICAL);
	}

	public static boolean hasQualifyingTorchflowerInRange(Level level, BlockPos origin, int horizontalRange, int verticalRange) {
		return findClosestMatch(
				origin,
				horizontalRange,
				verticalRange,
				pos -> isQualifyingTorchflower(level.getBlockState(pos))
		).isPresent();
	}

	public static boolean beesStayInHive(Level level, BlockPos searchOrigin, boolean vanillaStay) {
		return beesStayInHive(level, searchOrigin, vanillaStay, HIVE_NECTAR_RANGE, HIVE_NECTAR_VERTICAL);
	}

	public static boolean beesStayInHive(
			Level level,
			BlockPos searchOrigin,
			boolean vanillaStay,
			int horizontalRange,
			int verticalRange
	) {
		if (!vanillaStay) {
			return false;
		}
		if (isWeatherKeepingBeesInside(level)) {
			return true;
		}
		return !hasQualifyingTorchflowerInRange(level, searchOrigin, horizontalRange, verticalRange);
	}

	public static boolean beesStayInHiveForBee(Level level, @Nullable BlockPos hivePos, BlockPos beePos, boolean vanillaStay) {
		if (hivePos == null) {
			return vanillaStay;
		}
		return beesStayInHive(level, hivePos, vanillaStay);
	}

	public static boolean attractsBee(Level level, BlockPos beePos, BlockState state, boolean vanillaAttracts) {
		boolean vanillaStay = Boolean.TRUE.equals(
				level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.BEES_STAY_IN_HIVE, beePos)
		);
		return attractsBee(level, state, vanillaAttracts, vanillaStay);
	}

	public static boolean attractsBee(Level level, BlockState state, boolean vanillaAttracts, boolean vanillaStay) {
		boolean nightWorkWindow = vanillaStay && !isWeatherKeepingBeesInside(level);
		if (nightWorkWindow) {
			return isQualifyingTorchflower(state);
		}
		return vanillaAttracts;
	}

	public static int growCropDelay(int vanillaDelay, boolean pitcherPollen) {
		if (!pitcherPollen) {
			return vanillaDelay;
		}
		return Math.max(1, Math.round(vanillaDelay / PITCHER_POLLEN_MULTIPLIER));
	}

	public static boolean isOtherHiveOrNestInRange(Level level, BlockPos originHive) {
		return isOtherHiveOrNestInRange(level, originHive, HIVE_NECTAR_RANGE, HIVE_NECTAR_VERTICAL);
	}

	public static boolean isOtherHiveOrNestInRange(Level level, BlockPos originHive, int horizontalRange, int verticalRange) {
		return findClosestMatch(
				originHive,
				horizontalRange,
				verticalRange,
				pos -> !pos.equals(originHive) && isHiveOrNest(level.getBlockState(pos))
		).isPresent();
	}

	public static boolean isHiveOrNest(BlockState state) {
		return state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST);
	}

	public static boolean isHiveFull(@Nullable BeehiveBlockEntity hive) {
		return hive != null && hive.isFull();
	}

	/**
	 * Hive block entities only store bees that are currently inside. Regular members that are out
	 * collecting still point at the hive with {@link Bee#getHivePos()}. Registered occupancy is
	 * stored occupants plus those loaded members, so a hive is not treated as open just because a
	 * resident is on a pollen run.
	 */
	public static boolean hasRegisteredVacancy(int storedCount, int registeredOutsideCount) {
		return storedCount + registeredOutsideCount < BeehiveBlockEntity.MAX_OCCUPANTS;
	}

	public static int countRegisteredOutside(Level level, BlockPos hivePos, @Nullable Bee exclude) {
		AABB box = new AABB(hivePos).inflate(HIVE_RELOCATE_RANGE, HIVE_RELOCATE_VERTICAL, HIVE_RELOCATE_RANGE);
		int count = 0;
		for (Bee other : level.getEntitiesOfClass(Bee.class, box)) {
			if (other != exclude && hivePos.equals(other.getHivePos())) {
				count++;
			}
		}
		return count;
	}

	public static boolean hasRegisteredVacancy(Level level, BlockPos hivePos, @Nullable Bee exclude) {
		BeehiveBlockEntity hive = level.getBlockEntity(hivePos, net.minecraft.world.level.block.entity.BlockEntityTypes.BEEHIVE).orElse(null);
		if (hive == null) {
			return false;
		}
		return hasRegisteredVacancy(hive.getOccupantCount(), countRegisteredOutside(level, hivePos, exclude));
	}

	public static @Nullable BlockPos findHiveWithRegisteredVacancy(Level level, BlockPos originHive, Bee bee) {
		return findHiveWithRegisteredVacancy(level, originHive, bee, HIVE_RELOCATE_RANGE, HIVE_RELOCATE_VERTICAL);
	}

	public static @Nullable BlockPos findHiveWithRegisteredVacancy(
			Level level,
			BlockPos originHive,
			Bee bee,
			int horizontalRange,
			int verticalRange
	) {
		return findClosestMatch(
				originHive,
				horizontalRange,
				verticalRange,
				pos -> !pos.equals(originHive)
						&& isHiveOrNest(level.getBlockState(pos))
						&& hasRegisteredVacancy(level, pos, bee)
		).orElse(null);
	}

	public static boolean canFoundNestForAge(int age) {
		return age >= 0;
	}

	public static boolean canFoundNest(Bee bee) {
		return bee != null && canFoundNestForAge(bee.getAge());
	}

	public static boolean shouldStartFounding(Level level, BlockPos originHive, BeehiveBlockEntity hive, Bee bee) {
		return canFoundNest(bee) && isHiveFull(hive) && findHiveWithRegisteredVacancy(level, originHive, bee) == null;
	}

	public record FoundingSite(BlockPos nestPos, Direction facing) {
	}

	public static boolean isFarmlandOrCrop(BlockState state) {
		if (state.is(Blocks.FARMLAND)
				|| state.getBlock() instanceof CropBlock
				|| state.getBlock() instanceof StemBlock
				|| state.is(Blocks.SWEET_BERRY_BUSH)
				|| state.is(Blocks.CAVE_VINES)
				|| state.is(Blocks.CAVE_VINES_PLANT)) {
			return true;
		}
		return false;
	}

	public static boolean shouldAttemptFlyover(int tickCount, int entityId) {
		return Math.floorMod(tickCount + entityId, FLYOVER_INTERVAL_TICKS) == 0;
	}

	/**
	 * First surface below {@code beePos} that can take flyover pollen: a {@link BonemealableBlock}
	 * or a crop. Uses a downward clip (no height cap). Crops sitting on farmland are preferred
	 * over the farmland itself. Bare farmland is skipped.
	 */
	public static @Nullable BlockPos findFlyoverTarget(Level level, BlockPos beePos) {
		return findFlyoverTarget(level, Vec3.atCenterOf(beePos));
	}

	public static @Nullable BlockPos findFlyoverTarget(Level level, Vec3 from) {
		Vec3 to = new Vec3(from.x, level.getMinY(), from.z);
		BlockHitResult hit = level.clip(new ClipContext(
				from,
				to,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				CollisionContext.empty()
		));
		if (hit.getType() == HitResult.Type.MISS) {
			return null;
		}
		return resolveFlyoverTarget(level, hit.getBlockPos());
	}

	public static @Nullable BlockPos resolveFlyoverTarget(Level level, BlockPos hitPos) {
		BlockPos above = hitPos.above();
		BlockState aboveState = level.getBlockState(above);
		if (isFlyoverPollenTarget(aboveState)) {
			return above;
		}
		BlockState hitState = level.getBlockState(hitPos);
		if (isBareFarmland(hitState)) {
			return null;
		}
		if (isFlyoverPollenTarget(hitState)) {
			return hitPos;
		}
		return null;
	}

	public static boolean isFlyoverPollenTarget(BlockState state) {
		if (isBareFarmland(state)) {
			return false;
		}
		return state.getBlock() instanceof BonemealableBlock || isFarmlandOrCrop(state);
	}

	public static boolean isBareFarmland(BlockState state) {
		return state.is(Blocks.FARMLAND);
	}

	public static boolean tryFlyoverVegetation(ServerLevel level, Bee bee) {
		if (!bee.hasNectar()) {
			return false;
		}
		if (!shouldAttemptFlyover(bee.tickCount, bee.getId())) {
			return false;
		}
		BlockPos target = findFlyoverTarget(level, bee.position());
		if (target == null) {
			return false;
		}
		BlockState state = level.getBlockState(target);
		if (!(state.getBlock() instanceof BonemealableBlock growable)) {
			return false;
		}
		if (!growable.isValidBonemealTarget(level, target, state, BonemealSource.MOB)) {
			return false;
		}
		if (!growable.isBonemealSuccess(level, bee.getRandom(), target, state, BonemealSource.MOB)) {
			return false;
		}
		growable.performBonemeal(level, bee.getRandom(), target, state, BonemealSource.MOB);
		return true;
	}

	public static boolean isInFoundingBand(BlockPos origin, BlockPos pos) {
		int dx = Math.abs(pos.getX() - origin.getX());
		int dz = Math.abs(pos.getZ() - origin.getZ());
		int horiz = Math.max(dx, dz);
		int dy = Math.abs(pos.getY() - origin.getY());
		return horiz >= FOUNDING_MIN && horiz <= FOUNDING_MAX && dy <= HIVE_NECTAR_VERTICAL + 8;
	}

	public static boolean isWithinFoundingStay(BlockPos beePos, BlockPos target) {
		int dx = Math.abs(beePos.getX() - target.getX());
		int dy = Math.abs(beePos.getY() - target.getY());
		int dz = Math.abs(beePos.getZ() - target.getZ());
		return Math.max(dx, Math.max(dy, dz)) <= FOUNDING_STAY_RADIUS;
	}

	public static int nextFoundingDwell(int current, boolean inRadius) {
		return inRadius ? current + 1 : 0;
	}

	public static boolean foundingDwellComplete(int dwellTicks) {
		return dwellTicks >= FOUNDING_DWELL_TICKS;
	}

	public static @Nullable FoundingSite findFoundingSite(Level level, BlockPos origin, BlockPos near, RandomSource random) {
		FoundingSite local = scanFoundingSite(level, origin, near, 8);
		if (local != null) {
			return local;
		}
		for (int i = 0; i < 32; i++) {
			BlockPos probe = origin.offset(randomIntBand(random), random.nextInt(7) - 3, randomIntBand(random));
			FoundingSite site = scanFoundingSite(level, origin, probe, 2);
			if (site != null) {
				return site;
			}
		}
		return scanFoundingSite(level, origin, near, 2);
	}

	private static @Nullable FoundingSite scanFoundingSite(Level level, BlockPos origin, BlockPos near, int radius) {
		for (BlockPos pos : BlockPos.betweenClosed(near.offset(-radius, -radius, -radius), near.offset(radius, radius, radius))) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
				continue;
			}
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos nestPos = pos.relative(dir);
				if (isValidFoundingSite(level, origin, nestPos, dir)) {
					return new FoundingSite(nestPos.immutable(), dir);
				}
			}
		}
		return null;
	}

	public static boolean isValidFoundingSite(Level level, BlockPos origin, BlockPos nestPos, Direction facing) {
		if (!level.getBlockState(nestPos).isAir()) {
			return false;
		}
		if (!isInFoundingBand(origin, nestPos)) {
			return false;
		}
		BlockState support = level.getBlockState(nestPos.relative(facing.getOpposite()));
		return support.is(BlockTags.LOGS) || support.is(BlockTags.LEAVES);
	}

	public static boolean placeFoundingNest(ServerLevel level, FoundingSite site) {
		if (!level.getBlockState(site.nestPos()).isAir()) {
			return false;
		}
		return level.setBlockAndUpdate(
				site.nestPos(),
				Blocks.BEE_NEST.defaultBlockState().setValue(net.minecraft.world.level.block.BeehiveBlock.FACING, site.facing())
		);
	}

	public static boolean tryAttachNestToTree(ServerLevel level, BlockPos origin, BlockPos near) {
		FoundingSite site = scanFoundingSite(level, origin, near, 2);
		return site != null && placeFoundingNest(level, site);
	}

	public static BlockPos outwardWanderTarget(BlockPos origin, RandomSource random) {
		int mag = FOUNDING_MIN + 6;
		int dx = random.nextBoolean() ? mag : -mag;
		int dz = random.nextBoolean() ? mag : -mag;
		return origin.offset(dx, 0, dz);
	}

	public static void tickRelocate(ServerLevel level, Bee bee, BusyBeeData data) {
		if (!data.busyBees$isRelocating()) {
			return;
		}
		BlockPos dest = data.busyBees$relocateTarget();
		if (dest == null || !isHiveOrNest(level.getBlockState(dest))) {
			retryRelocateOrFound(level, bee, data, data.busyBees$originHive());
			return;
		}
		if (!bee.blockPosition().closerThan(dest, 2) || bee.tickCount % 10 == 0) {
			bee.getNavigation().moveTo(dest.getX() + 0.5, dest.getY() + 0.5, dest.getZ() + 0.5, 1.0);
		}
		if (!bee.blockPosition().closerThan(dest, 2)) {
			return;
		}
		BeehiveBlockEntity hive = level.getBlockEntity(dest, net.minecraft.world.level.block.entity.BlockEntityTypes.BEEHIVE).orElse(null);
		if (hive != null && !hive.isFull()) {
			data.busyBees$clearRelocate();
			hive.addOccupant(bee);
			return;
		}
		bee.setHivePos(null);
		retryRelocateOrFound(level, bee, data, dest);
	}

	private static void retryRelocateOrFound(ServerLevel level, Bee bee, BusyBeeData data, @Nullable BlockPos searchOrigin) {
		BlockPos origin = searchOrigin != null ? searchOrigin : bee.blockPosition();
		BlockPos next = findHiveWithRegisteredVacancy(level, origin, bee);
		if (next != null) {
			data.busyBees$beginRelocate(origin, next);
			bee.setHivePos(next);
			return;
		}
		data.busyBees$clearRelocate();
		if (canFoundNest(bee)) {
			data.busyBees$beginFounding(origin);
		}
	}

	public static void tickFounding(ServerLevel level, Bee bee, BusyBeeData data) {
		if (!data.busyBees$isFounding() || data.busyBees$originHive() == null) {
			return;
		}
		if (bee.isBaby()) {
			data.busyBees$clearFounding();
			return;
		}
		BlockPos origin = data.busyBees$originHive();
		BlockPos target = data.busyBees$foundingTarget();
		Direction facing = data.busyBees$foundingFacing();
		if (target == null || facing == null || !isValidFoundingSite(level, origin, target, facing)) {
			FoundingSite site = findFoundingSite(level, origin, bee.blockPosition(), bee.getRandom());
			if (site == null) {
				data.busyBees$setFoundingTarget(null);
				data.busyBees$setFoundingFacing(null);
				data.busyBees$setFoundingDwell(0);
				if (bee.tickCount % 20 == 0) {
					BlockPos wander = outwardWanderTarget(origin, bee.getRandom());
					bee.getNavigation().moveTo(wander.getX() + 0.5, bee.getY(), wander.getZ() + 0.5, 1.0);
				}
				return;
			}
			data.busyBees$setFoundingTarget(site.nestPos());
			data.busyBees$setFoundingFacing(site.facing());
			data.busyBees$setFoundingDwell(0);
			target = site.nestPos();
			facing = site.facing();
		}
		boolean inRadius = isWithinFoundingStay(bee.blockPosition(), target);
		int dwell = nextFoundingDwell(data.busyBees$foundingDwell(), inRadius);
		data.busyBees$setFoundingDwell(dwell);
		if (!inRadius || bee.tickCount % 10 == 0) {
			bee.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 1.0);
		}
		if (!foundingDwellComplete(dwell)) {
			return;
		}
		FoundingSite site = new FoundingSite(target, facing);
		if (!placeFoundingNest(level, site)) {
			data.busyBees$setFoundingTarget(null);
			data.busyBees$setFoundingFacing(null);
			data.busyBees$setFoundingDwell(0);
			return;
		}
		bee.setHivePos(target);
		BeehiveBlockEntity nest = level.getBlockEntity(target, net.minecraft.world.level.block.entity.BlockEntityTypes.BEEHIVE).orElse(null);
		data.busyBees$clearFounding();
		if (nest != null) {
			nest.addOccupant(bee);
		}
	}

	private static int randomIntBand(RandomSource random) {
		int mag = FOUNDING_MIN + random.nextInt(FOUNDING_MAX - FOUNDING_MIN + 1);
		return random.nextBoolean() ? mag : -mag;
	}
}
