package busy_bees.mixin;

import busy_bees.BusyBeeData;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BaseBeeGoal")
public abstract class BeeBaseBeeGoalMixin {

	@Shadow
	@Final
	Bee this$0;

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
	private void busyBees$holdFoundingSite(CallbackInfoReturnable<Boolean> ci) {
		if (this.this$0 instanceof BusyBeeData data && (data.busyBees$isFounding() || data.busyBees$isRelocating())) {
			ci.setReturnValue(false);
		}
	}
}
