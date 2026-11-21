package com.basalto.ui;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.blaze3d.textures.GpuTexture;

/** Texturas de letras das fontes do Basalto (desenhadas com filtro linear). Preenchido pelo FontTextureMixin. */
public final class FontTextures {
	public static final Set<GpuTexture> BASALTO = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	private FontTextures() {}
}
