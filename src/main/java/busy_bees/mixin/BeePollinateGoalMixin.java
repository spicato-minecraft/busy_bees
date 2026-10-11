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
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeePollinateGoal")
public abstract class BeePollinateGoalMixin {

	@Shadow
	@Final
	Bee this$0;

	@ModifyArg(
			method = "findNearbyFlower",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/blockscan/OrderedBlockMatcher;filterState(Ljava/util/function/Predicate;)Lnet/minecraft/world/level/blockscan/OrderedBlockMatcher;"
			)
	)
	private Predicate<BlockState> busyBees$nightTargets(Predicate<BlockState> vanilla) {
		return state -> BusyBeeSupport.attractsBee(
				this.this$0.level(),
				this.this$0.blockPosition(),
				state,
				vanilla.test(state)
		);
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
