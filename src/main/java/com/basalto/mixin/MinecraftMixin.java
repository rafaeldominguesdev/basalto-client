package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.render.ESP;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** ESP: faz as entidades escolhidas brilharem através das paredes. */
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 0)
	private void basalto$esp(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ModuleManager.on(ESP.class) && Basalto.modules().get(ESP.class).shouldGlow(entity)) cir.setReturnValue(true);
	}
}
