package busy_bees.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import busy_bees.BusyBeeData;
import busy_bees.BusyBeeSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeePollinateGoal")
public abstract class BeePollinateGoalMixin {

	@Shadow
	@Final
	Bee field_20377;

	@WrapOperation(
			method = "findNearbyFlower",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/animal/bee/Bee;attractsBees(Lnet/minecraft/world/level/block/state/BlockState;)Z"
			)
	)
	private boolean busyBees$nightTargets(BlockState state, Operation<Boolean> original) {
		return BusyBeeSupport.attractsBee(this.field_20377.level(), this.field_20377.blockPosition(), state, original.call(state));
	}

	@WrapOperation(
			method = "stop",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/animal/bee/Bee;setHasNectar(Z)V"
			)
	)
	private void busyBees$pitcherNectar(Bee bee, boolean hasNectar, Operation<Void> original) {
		original.call(bee, hasNectar);
		if (!hasNectar) {
			return;
		}
		BlockPos flower = bee.getSavedFlowerPos();
		if (flower != null && BusyBeeSupport.isMaturePitcherPlant(bee.level().getBlockState(flower))) {
			((BusyBeeData) bee).busyBees$setPitcherPollen(true);
		}
	}
}
