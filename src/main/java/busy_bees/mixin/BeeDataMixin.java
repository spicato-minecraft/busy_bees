package busy_bees.mixin;

import busy_bees.BusyBeeData;
import busy_bees.BusyBeeSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

	@Unique
	@Nullable
	private BlockPos busyBees$foundingTarget;

	@Unique
	@Nullable
	private Direction busyBees$foundingFacing;

	@Unique
	private int busyBees$foundingDwell;

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

	@Override
	public @Nullable BlockPos busyBees$foundingTarget() {
		return this.busyBees$foundingTarget;
	}

	@Override
	public void busyBees$setFoundingTarget(@Nullable BlockPos target) {
		this.busyBees$foundingTarget = target;
	}

	@Override
	public @Nullable Direction busyBees$foundingFacing() {
		return this.busyBees$foundingFacing;
	}

	@Override
	public void busyBees$setFoundingFacing(@Nullable Direction facing) {
		this.busyBees$foundingFacing = facing;
	}

	@Override
	public int busyBees$foundingDwell() {
		return this.busyBees$foundingDwell;
	}

	@Override
	public void busyBees$setFoundingDwell(int ticks) {
		this.busyBees$foundingDwell = ticks;
	}

	@Override
	public void busyBees$beginFounding(BlockPos origin) {
		this.busyBees$founding = true;
		this.busyBees$originHive = origin;
		this.busyBees$foundingTarget = null;
		this.busyBees$foundingFacing = null;
		this.busyBees$foundingDwell = 0;
	}

	@Override
	public void busyBees$clearFounding() {
		this.busyBees$founding = false;
		this.busyBees$originHive = null;
		this.busyBees$foundingTarget = null;
		this.busyBees$foundingFacing = null;
		this.busyBees$foundingDwell = 0;
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
		output.storeNullable("busy_bees.target", BlockPos.CODEC, this.busyBees$foundingTarget);
		output.storeNullable("busy_bees.facing", Direction.CODEC, this.busyBees$foundingFacing);
		output.putInt("busy_bees.dwell", this.busyBees$foundingDwell);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void busyBees$load(ValueInput input, CallbackInfo ci) {
		this.busyBees$pitcherPollen = input.getBooleanOr("busy_bees.pitcher", false);
		this.busyBees$founding = input.getBooleanOr("busy_bees.founding", false);
		this.busyBees$originHive = input.read("busy_bees.origin", BlockPos.CODEC).orElse(null);
		this.busyBees$foundingTarget = input.read("busy_bees.target", BlockPos.CODEC).orElse(null);
		this.busyBees$foundingFacing = input.read("busy_bees.facing", Direction.CODEC).orElse(null);
		this.busyBees$foundingDwell = input.getIntOr("busy_bees.dwell", 0);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void busyBees$tickGameplay(CallbackInfo ci) {
		Bee bee = (Bee) (Object) this;
		if (!(bee.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		BusyBeeSupport.tryFlyoverVegetation(serverLevel, bee);
		BusyBeeSupport.tickFounding(serverLevel, bee, this);
	}
}
