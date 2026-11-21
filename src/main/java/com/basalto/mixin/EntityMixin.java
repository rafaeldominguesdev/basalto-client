package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.combat.Velocity;
import com.basalto.modules.render.ESP;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Velocity: reduz o knockback que o servidor manda para você. */
	@ModifyVariable(method = "lerpMotion(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), argsOnly = true)
	private Vec3 basalto$velocity(Vec3 motion) {
		Entity self = (Entity) (Object) this;
		if (self != Minecraft.getInstance().player || !ModuleManager.on(Velocity.class)) return motion;
		return Basalto.modules().get(Velocity.class).modify(motion, self.getDeltaMovement());
	}

	/** ESP: cor do contorno. */
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
	private void basalto$espColor(CallbackInfoReturnable<Integer> cir) {
		if (!ModuleManager.on(ESP.class)) return;
		ESP esp = Basalto.modules().get(ESP.class);
		Entity self = (Entity) (Object) this;
		if (esp.shouldGlow(self)) cir.setReturnValue(esp.color(self));
	}
}
