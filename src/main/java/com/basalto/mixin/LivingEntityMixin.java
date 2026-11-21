package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.movement.Step;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Step: sobe blocos mais altos sem pular. */
	@ModifyReturnValue(method = "maxUpStep()F", at = @At("RETURN"))
	private float basalto$step(float original) {
		if ((Object) this != Minecraft.getInstance().player || !ModuleManager.on(Step.class)) return original;
		return Math.max(original, Basalto.modules().get(Step.class).height.getF());
	}
}
