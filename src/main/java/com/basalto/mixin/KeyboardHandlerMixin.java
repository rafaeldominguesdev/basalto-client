package com.basalto.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.basalto.Basalto;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;

/** Escuta as teclas para abrir o menu e ligar/desligar módulos. */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"))
	private void basalto$onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (action == GLFW.GLFW_PRESS) Basalto.onKey(event.key());
	}
}
