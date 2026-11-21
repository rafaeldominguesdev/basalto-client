package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.render.XRay;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** XRay no Indigo (o renderizador da Fabric API, usado no terreno). */
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl", remap = false)
public abstract class IndigoBlockRendererMixin {
	@Shadow
	private BlockState blockState;

	@Inject(method = "shouldCullFace", at = @At("HEAD"), require = 0, cancellable = true)
	private void basalto$xray(Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (!ModuleManager.on(XRay.class) || blockState == null) return;
		Boolean draw = Basalto.modules().get(XRay.class).shouldDrawSide(blockState);
		if (draw != null) cir.setReturnValue(!draw);
	}
}
