package busy_bees.mixin;

import busy_bees.BusyBeeData;
import busy_bees.BusyBeeSupport;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeGrowCropGoal")
public abstract class BeeGrowCropGoalMixin {

	@Shadow
	@Final
	Bee this$0;

	@ModifyArg(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I")
	)
	private int busyBees$pitcherGrowRate(int bound) {
		boolean pitcher = ((BusyBeeData) this.this$0).busyBees$hasPitcherPollen();
		return BusyBeeSupport.growCropDelay(bound, pitcher);
	}
}
