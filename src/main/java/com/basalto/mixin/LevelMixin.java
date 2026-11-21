package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.basalto.module.ModuleManager;
import com.basalto.modules.render.NoWeather;

import net.minecraft.world.level.Level;

@Mixin(Level.class)
public abstract class LevelMixin {
	@Inject(method = "getRainLevel(F)F", at = @At("HEAD"), cancellable = true)
	private void basalto$noRain(float delta, CallbackInfoReturnable<Float> cir) {
		if (((Level) (Object) this).isClientSide() && ModuleManager.on(NoWeather.class)) cir.setReturnValue(0F);
	}
}
