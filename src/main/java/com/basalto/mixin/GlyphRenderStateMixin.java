package com.basalto.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;

/**
 * As fontes do Basalto são rasterizadas com o dobro da resolução; aqui elas
 * ganham filtro linear para reduzir sem serrilhado. A fonte do Minecraft continua "nearest".
 */
@Mixin(GlyphRenderState.class)
public abstract class GlyphRenderStateMixin {
	@Shadow
	@Final
	private TextRenderable renderable;

	@Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
	private void basalto$linearFonts(CallbackInfoReturnable<TextureSetup> cir) {
		GpuTextureView view = renderable.textureView();
		if (view != null && com.basalto.ui.FontTextures.BASALTO.contains(view.texture()))
			cir.setReturnValue(TextureSetup.singleTextureWithLightmap(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
	}
}
