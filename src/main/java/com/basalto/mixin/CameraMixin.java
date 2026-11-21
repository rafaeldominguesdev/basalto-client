package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.render.Zoom;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Camera;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyReturnValue(method = "calculateFov(F)F", at = @At("RETURN"))
	private float basalto$zoom(float original) {
		if (!ModuleManager.on(Zoom.class)) return original;
		return Basalto.modules().get(Zoom.class).apply(original);
	}
}
