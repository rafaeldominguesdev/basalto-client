package com.basalto.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;

/** Marca as texturas de letras das fontes do Basalto (o nome delas começa com "basalto:"). */
@Mixin(FontTexture.class)
public abstract class FontTextureMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void basalto$mark(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored, CallbackInfo ci) {
		if (label.get().startsWith("basalto:")) com.basalto.ui.FontTextures.BASALTO.add(((FontTexture) (Object) this).getTexture());
	}
}
