package busy_bees.mixin;

import busy_bees.BusyBeeData;
import busy_bees.BusyBeeSupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeEnterHiveGoal")
public abstract class BeeEnterHiveGoalMixin {

	@WrapOperation(
			method = "canBeeUse",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/world/entity/animal/bee/Bee;hivePos:Lnet/minecraft/core/BlockPos;",
					opcode = org.objectweb.asm.Opcodes.PUTFIELD
			)
	)
	private void busyBees$overflowFounding(Bee bee, BlockPos newHivePos, Operation<Void> original) {
		BlockPos origin = bee.getHivePos();
		if (origin != null && newHivePos == null) {
			BeehiveBlockEntity hive = bee.level().getBlockEntity(origin, net.minecraft.world.level.block.entity.BlockEntityType.BEEHIVE).orElse(null);
			if (hive != null && BusyBeeSupport.shouldStartFounding(bee.level(), origin, hive)) {
				BusyBeeData data = (BusyBeeData) bee;
				data.busyBees$setFounding(true);
				data.busyBees$setOriginHive(origin);
			}
		}
		original.call(bee, newHivePos);
	}
}
