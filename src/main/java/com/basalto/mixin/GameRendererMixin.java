package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.basalto.module.ModuleManager;
import com.basalto.modules.render.Fullbright;
import com.basalto.modules.render.NoHurtCam;
import com.basalto.modules.render.XRay;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/** Fullbright / XRay: visão noturna no máximo. */
	@Inject(method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F", at = @At("HEAD"), cancellable = true)
	private static void basalto$nightVision(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
		if (ModuleManager.on(Fullbright.class) || ModuleManager.on(XRay.class)) cir.setReturnValue(1F);
	}

	/** NoHurtCam: sem tremor ao levar dano. */
	@Inject(method = "bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
		at = @At("HEAD"), cancellable = true)
	private void basalto$noHurtCam(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
		if (ModuleManager.on(NoHurtCam.class)) ci.cancel();
	}
}
