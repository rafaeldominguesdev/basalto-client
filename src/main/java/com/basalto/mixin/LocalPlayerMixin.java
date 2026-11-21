package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.movement.NoSlow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	/** Roda os módulos no começo do tick do jogador, antes do movimento. */
	@Inject(method = "tick()V", at = @At("HEAD"))
	private void basalto$onTick(CallbackInfo ci) {
		Basalto.onPlayerTick();
	}

	/** NoSlow: não fica lento usando itens. */
	@WrapOperation(method = "aiStep()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"),
		require = 0)
	private boolean basalto$noSlow(LocalPlayer instance, Operation<Boolean> original) {
		if (ModuleManager.on(NoSlow.class)) return false;
		return original.call(instance);
	}
}
