package busy_bees;

import busy_bees.mixin.BeehiveBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Hive occupants are NBT snapshots, not live entities. Entity ids and UUIDs are not kept.
 * Love and cooldown are written onto that occupant NBT in place ({@code InLove}, {@code Age}),
 * matching vanilla {@code Animal.mobInteract}. Babies can still be age-up fed while stored.
 */
public final class BusyBeeHiveFeeding {

	public static final int LOVE_TICKS = 600;
	public static final int BREED_COOLDOWN_AGE = 6000;
	public static final int BABY_AGE = -24000;

	private BusyBeeHiveFeeding() {
	}

	public static boolean tryUseFlowerOnHive(
			Level level,
			BlockPos hivePos,
			Player player,
			ItemStack stack
	) {
		if (!stack.is(ItemTags.BEE_FOOD)) {
			return false;
		}
		if (!(level.getBlockEntity(hivePos) instanceof BeehiveBlockEntity hive) || hive.isEmpty()) {
			return false;
		}
		BeehiveBlockEntityAccessor stored = (BeehiveBlockEntityAccessor) hive;
		List<Occupant> occupants = new ArrayList<>(stored.busyBees$getBees());
		if (level.isClientSide()) {
			return canFeedOccupants(occupants);
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		int storedBefore = hive.getOccupantCount();
		FeedOutcome result = feedOccupants(occupants);
		if (result == FeedOutcome.NONE) {
			return false;
		}
		if (occupants.size() > BeehiveBlockEntity.MAX_OCCUPANTS) {
			Occupant overflow = occupants.remove(occupants.size() - 1);
			spawnBabyOutside(serverLevel, hivePos, level.getBlockState(hivePos), overflow);
		} else if (occupants.size() > storedBefore) {
			for (int i = storedBefore; i < occupants.size(); i++) {
				hive.storeBee(occupants.get(i));
			}
		}
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		if (player instanceof ServerPlayer serverPlayer && result == FeedOutcome.BRED) {
			serverPlayer.awardStat(Stats.ANIMALS_BRED);
		}
		serverLevel.playSound(null, hivePos, SoundEvents.BEE_POLLINATE, SoundSource.BLOCKS, 1.0F, 1.0F);
		serverLevel.sendParticles(
				result == FeedOutcome.AGED ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.HEART,
				hivePos.getX() + 0.5,
				hivePos.getY() + 0.9,
				hivePos.getZ() + 0.5,
				4,
				0.25,
				0.2,
				0.25,
				0.0
		);
		level.gameEvent(player, GameEvent.EAT, hivePos);
		hive.setChanged();
		return true;
	}

	public enum FeedOutcome {
		NONE,
		FED,
		AGED,
		BRED
	}

	public static boolean canFeedOccupants(List<Occupant> occupants) {
		return indexOfFeedableAdult(occupants) >= 0 || indexOfAgeableBaby(occupants) >= 0;
	}

	public static FeedOutcome feedOccupants(List<Occupant> occupants) {
		int feedIndex = indexOfFeedableAdult(occupants);
		if (feedIndex < 0) {
			feedIndex = indexOfAgeableBaby(occupants);
			if (feedIndex < 0) {
				return FeedOutcome.NONE;
			}
			ageUpBaby(occupants.get(feedIndex));
			return FeedOutcome.AGED;
		}
		withLove(occupants.get(feedIndex));
		int firstLove = -1;
		int secondLove = -1;
		for (int i = 0; i < occupants.size(); i++) {
			if (!isInLoveAdult(occupants.get(i))) {
				continue;
			}
			if (firstLove < 0) {
				firstLove = i;
			} else {
				secondLove = i;
				break;
			}
		}
		if (secondLove < 0) {
			return FeedOutcome.FED;
		}
		withBreedCooldown(occupants.get(firstLove));
		withBreedCooldown(occupants.get(secondLove));
		occupants.add(babyOccupant());
		return FeedOutcome.BRED;
	}

	public static int occupantAge(Occupant occupant) {
		return occupantTag(occupant).getIntOr("Age", 0);
	}

	public static int occupantInLove(Occupant occupant) {
		return occupantTag(occupant).getIntOr("InLove", 0);
	}

	public static boolean isAdult(Occupant occupant) {
		return occupantAge(occupant) == 0;
	}

	/**
	 * Vanilla {@code Animal.mobInteract}: adult food only when {@code getAge() == 0} and not already in love.
	 * Age &gt; 0 is the post-breed cooldown; InLove &gt; 0 is the current love window.
	 */
	public static boolean canFallInLove(Occupant occupant) {
		return occupantAge(occupant) == 0
				&& occupantInLove(occupant) <= 0
				&& !occupantTag(occupant).getBooleanOr("AgeLocked", false);
	}

	public static boolean isInLoveAdult(Occupant occupant) {
		return occupantAge(occupant) == 0 && occupantInLove(occupant) > 0;
	}

	private static int indexOfFeedableAdult(List<Occupant> occupants) {
		for (int i = 0; i < occupants.size(); i++) {
			if (canFallInLove(occupants.get(i))) {
				return i;
			}
		}
		return -1;
	}

	private static Occupant withLove(Occupant occupant) {
		occupantTag(occupant).putInt("InLove", LOVE_TICKS);
		return occupant;
	}

	private static Occupant withBreedCooldown(Occupant occupant) {
		CompoundTag tag = occupantTag(occupant);
		tag.putInt("Age", BREED_COOLDOWN_AGE);
		tag.putInt("InLove", 0);
		return occupant;
	}

	private static void ageUpBaby(Occupant occupant) {
		CompoundTag tag = occupantTag(occupant);
		int age = tag.getIntOr("Age", 0);
		int seconds = AgeableMob.getSpeedUpSecondsWhenFeeding(-age);
		tag.putInt("Age", Math.min(0, age + seconds * 20));
	}

	private static int indexOfAgeableBaby(List<Occupant> occupants) {
		for (int i = 0; i < occupants.size(); i++) {
			Occupant occupant = occupants.get(i);
			if (occupantAge(occupant) < 0 && !occupantTag(occupant).getBooleanOr("AgeLocked", false)) {
				return i;
			}
		}
		return -1;
	}

	private static CompoundTag occupantTag(Occupant occupant) {
		return occupant.entityData().getUnsafe();
	}

	public static Occupant babyOccupant() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Age", BABY_AGE);
		return new Occupant(TypedEntityData.of(EntityTypes.BEE, tag), 0, 600);
	}

	private static void spawnBabyOutside(ServerLevel level, BlockPos hivePos, BlockState state, Occupant occupant) {
		BlockPos spawnPos = hivePos;
		if (state.hasProperty(BeehiveBlock.FACING)) {
			spawnPos = hivePos.relative(state.getValue(BeehiveBlock.FACING));
		}
		Bee baby = EntityTypes.BEE.create(level, EntitySpawnReason.BREEDING);
		if (baby == null) {
			return;
		}
		occupant.entityData().loadInto(baby);
		baby.setBaby(true);
		baby.setHivePos(hivePos);
		baby.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(baby);
	}
}
