package busy_bees.mixin;

import busy_bees.BusyBeeData;
import busy_bees.BusyBeeSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bee.class)
public abstract class BeeDataMixin implements BusyBeeData {

	@Unique
	private boolean busyBees$pitcherPollen;

	@Unique
	private boolean busyBees$founding;

	@Unique
	@Nullable
	private BlockPos busyBees$originHive;

	@Override
	public boolean busyBees$hasPitcherPollen() {
		return this.busyBees$pitcherPollen;
	}

	@Override
	public void busyBees$setPitcherPollen(boolean pitcherPollen) {
		this.busyBees$pitcherPollen = pitcherPollen;
	}

	@Override
	public boolean busyBees$isFounding() {
		return this.busyBees$founding;
	}

	@Override
	public void busyBees$setFounding(boolean founding) {
		this.busyBees$founding = founding;
	}

	@Override
	public @Nullable BlockPos busyBees$originHive() {
		return this.busyBees$originHive;
	}

	@Override
	public void busyBees$setOriginHive(@Nullable BlockPos origin) {
		this.busyBees$originHive = origin;
	}

	@Inject(method = "setHasNectar", at = @At("TAIL"))
	private void busyBees$clearPitcherWhenNectarDrops(boolean hasNectar, CallbackInfo ci) {
		if (!hasNectar) {
			this.busyBees$pitcherPollen = false;
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void busyBees$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean("busy_bees.pitcher", this.busyBees$pitcherPollen);
		output.putBoolean("busy_bees.founding", this.busyBees$founding);
		output.storeNullable("busy_bees.origin", BlockPos.CODEC, this.busyBees$originHive);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void busyBees$load(ValueInput input, CallbackInfo ci) {
		this.busyBees$pitcherPollen = input.getBooleanOr("busy_bees.pitcher", false);
		this.busyBees$founding = input.getBooleanOr("busy_bees.founding", false);
		this.busyBees$originHive = input.read("busy_bees.origin", BlockPos.CODEC).orElse(null);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void busyBees$tickGameplay(CallbackInfo ci) {
		Bee bee = (Bee) (Object) this;
		if (!(bee.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		BusyBeeSupport.tryFlyoverVegetation(serverLevel, bee);
		if (!this.busyBees$founding || this.busyBees$originHive == null) {
			return;
		}
		BlockPos origin = this.busyBees$originHive;
		if (!BusyBeeSupport.isInFoundingBand(origin, bee.blockPosition())) {
			if (bee.tickCount % 20 == 0) {
				BlockPos wander = BusyBeeSupport.outwardWanderTarget(origin, bee.getRandom());
				bee.getNavigation().moveTo(wander.getX() + 0.5, bee.getY(), wander.getZ() + 0.5, 1.0);
			}
			return;
		}
		if (BusyBeeSupport.tryPlaceFoundingNest(serverLevel, origin, bee.blockPosition(), bee.getRandom())) {
			BlockPos nest = BlockPos.findClosestMatch(
					bee.blockPosition(),
					4,
					4,
					pos -> serverLevel.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEE_NEST)
			).orElse(null);
			if (nest != null) {
				bee.setHivePos(nest);
				this.busyBees$founding = false;
				this.busyBees$originHive = null;
			}
		}
	}
}
