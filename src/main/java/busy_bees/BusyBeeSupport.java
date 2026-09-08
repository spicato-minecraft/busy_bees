package busy_bees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class BusyBeeSupport {

	public static final int HIVE_NECTAR_RANGE = 22;
	public static final int HIVE_NECTAR_VERTICAL = 8;
	public static final int FOUNDING_MIN = 22;
	public static final int FOUNDING_MAX = 40;
	public static final float PITCHER_POLLEN_MULTIPLIER = 1.3F;
	/** Vanilla BeeGrowCropGoal uses delay 30; 30/1.3 ≈ 23 for 1.3× attempts. */
	public static final int VANILLA_GROW_CHANCE_DELAY = 30;

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

	public static boolean hasQualifyingTorchflowerInRange(Level level, BlockPos origin) {
		return hasQualifyingTorchflowerInRange(level, origin, HIVE_NECTAR_RANGE, HIVE_NECTAR_VERTICAL);
	}

	public static boolean hasQualifyingTorchflowerInRange(Level level, BlockPos origin, int horizontalRange, int verticalRange) {
		return BlockPos.findClosestMatch(
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
		return BlockPos.findClosestMatch(
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

	public static boolean shouldStartFounding(Level level, BlockPos originHive, BeehiveBlockEntity hive) {
		return isHiveFull(hive) && !isOtherHiveOrNestInRange(level, originHive);
	}

	public static boolean isFarmlandOrCrop(BlockState state) {
		if (state.is(Blocks.FARMLAND)
				|| state.getBlock() instanceof FarmBlock
				|| state.getBlock() instanceof CropBlock
				|| state.getBlock() instanceof StemBlock
				|| state.is(Blocks.SWEET_BERRY_BUSH)
				|| state.is(Blocks.CAVE_VINES)
				|| state.is(Blocks.CAVE_VINES_PLANT)) {
			return true;
		}
		return false;
	}

	public static boolean tryFlyoverVegetation(ServerLevel level, Bee bee) {
		if (!bee.hasNectar()) {
			return false;
		}
		if (bee.tickCount % 40 != 0) {
			return false;
		}
		if (bee.getRandom().nextInt(8) != 0) {
			return false;
		}
		BlockPos below = bee.blockPosition().below();
		BlockState state = level.getBlockState(below);
		if (isFarmlandOrCrop(state)) {
			return false;
		}
		if (!(state.getBlock() instanceof BonemealableBlock growable)) {
			return false;
		}
		if (!growable.isValidBonemealTarget(level, below, state)) {
			return false;
		}
		if (!growable.isBonemealSuccess(level, level.random, below, state)) {
			return false;
		}
		growable.performBonemeal(level, level.random, below, state);
		return true;
	}

	public static boolean isInFoundingBand(BlockPos origin, BlockPos pos) {
		int dx = Math.abs(pos.getX() - origin.getX());
		int dz = Math.abs(pos.getZ() - origin.getZ());
		int horiz = Math.max(dx, dz);
		int dy = Math.abs(pos.getY() - origin.getY());
		return horiz >= FOUNDING_MIN && horiz <= FOUNDING_MAX && dy <= HIVE_NECTAR_VERTICAL + 8;
	}

	public static boolean tryPlaceFoundingNest(ServerLevel level, BlockPos origin, BlockPos searchCenter, RandomSource random) {
		for (int i = 0; i < 24; i++) {
			int dx = randomIntBand(random);
			int dz = randomIntBand(random);
			int dy = random.nextInt(7) - 3;
			BlockPos probe = searchCenter.offset(dx, dy, dz);
			if (!isInFoundingBand(origin, probe) && !isInFoundingBand(origin, searchCenter)) {
				continue;
			}
			if (tryAttachNestToTree(level, origin, probe)) {
				return true;
			}
		}
		return tryAttachNestToTree(level, origin, searchCenter);
	}

	private static int randomIntBand(RandomSource random) {
		int mag = FOUNDING_MIN + random.nextInt(FOUNDING_MAX - FOUNDING_MIN + 1);
		return random.nextBoolean() ? mag : -mag;
	}

	public static boolean tryAttachNestToTree(ServerLevel level, BlockPos origin, BlockPos near) {
		for (BlockPos pos : BlockPos.betweenClosed(near.offset(-2, -2, -2), near.offset(2, 2, 2))) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
				continue;
			}
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos nestPos = pos.relative(dir);
				if (!level.getBlockState(nestPos).isAir()) {
					continue;
				}
				if (!isInFoundingBand(origin, nestPos)) {
					continue;
				}
				level.setBlockAndUpdate(
						nestPos,
						Blocks.BEE_NEST.defaultBlockState().setValue(net.minecraft.world.level.block.BeehiveBlock.FACING, dir)
				);
				return true;
			}
		}
		return false;
	}

	public static BlockPos outwardWanderTarget(BlockPos origin, RandomSource random) {
		int mag = FOUNDING_MIN + 6;
		int dx = random.nextBoolean() ? mag : -mag;
		int dz = random.nextBoolean() ? mag : -mag;
		return origin.offset(dx, 0, dz);
	}
}
