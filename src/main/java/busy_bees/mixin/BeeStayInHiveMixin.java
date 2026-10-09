package busy_bees.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import busy_bees.BusyBeeSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.class)
public abstract class BeeStayInHiveMixin {

	@Shadow
	@Nullable
	BlockPos hivePos;

	@WrapOperation(
			method = "wantsToEnterHive",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Object;"
			)
	)
	private Object busyBees$nightTorchflowerStay(
			EnvironmentAttributeSystem reader,
			EnvironmentAttribute<?> attribute,
			Vec3 pos,
			Operation<Object> original
	) {
		Object vanilla = original.call(reader, attribute, pos);
		Bee bee = (Bee) (Object) this;
		boolean vanillaStay = Boolean.TRUE.equals(vanilla);
		return BusyBeeSupport.beesStayInHiveForBee(bee.level(), this.hivePos, bee.blockPosition(), vanillaStay);
	}

	@Inject(method = "doesHiveHaveSpace", at = @At("HEAD"), cancellable = true)
	private void busyBees$registeredVacancy(BlockPos hivePos, CallbackInfoReturnable<Boolean> cir) {
		Bee bee = (Bee) (Object) this;
		cir.setReturnValue(BusyBeeSupport.hasRegisteredVacancy(bee.level(), hivePos, bee));
	}
}
