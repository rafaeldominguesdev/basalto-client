package com.basalto.ui;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Escala tipográfica (Manrope). Tamanho e altura de linha em unidades da interface.
 * Precisa casar com tools/ui-assets/build_fonts.py.
 */
public enum Type {
	CAPTION("caption", 6.5f, 10),
	LABEL("label", 7f, 11),
	BODY("body", 7f, 11),
	BODY_MEDIUM("body_medium", 7f, 11),
	BODY_SEMI("body_semi", 7f, 11),
	TITLE("title", 8.5f, 12),
	HEADING("heading", 11f, 15),
	METRIC("metric", 14f, 18),
	BRAND("brand", 8f, 11);

	/** Densidades (pixels por unidade) que têm fonte gerada. */
	public static final float[] DENSITIES = Densities.VALUES;

	public final String id;
	public final float size;
	public final float lineHeight;
	/** Altura das maiúsculas da Manrope (0,72 em). */
	public final float capHeight;
	private final FontDescription[] fonts = new FontDescription[Densities.VALUES.length];

	Type(String id, float size, float lineHeight) {
		this.id = id;
		this.size = size;
		this.lineHeight = lineHeight;
		this.capHeight = size * 0.72f;
		for (int i = 0; i < Densities.VALUES.length; i++)
			fonts[i] = new FontDescription.Resource(Identifier.fromNamespaceAndPath("basalto", id + "_" + Math.round(Densities.VALUES[i] * 100)));
	}

	public FontDescription font(int densityIndex) {
		return fonts[densityIndex];
	}

	private static final class Densities {
		static final float[] VALUES = {1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f};
	}
}
