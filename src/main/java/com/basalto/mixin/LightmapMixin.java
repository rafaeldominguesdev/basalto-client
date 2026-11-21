package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.basalto.module.ModuleManager;
import com.basalto.modules.render.Fullbright;
import com.basalto.modules.render.XRay;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

/**
 * Fullbright / XRay: visão noturna no máximo. No 26.1.2 o jogo só lê a escala
 * da visão noturna se o jogador tiver o efeito, então o valor é forçado aqui.
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMixin {
	@Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
	private void basalto$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (ModuleManager.on(Fullbright.class) || ModuleManager.on(XRay.class)) state.nightVisionEffectIntensity = 1F;
	}
}
