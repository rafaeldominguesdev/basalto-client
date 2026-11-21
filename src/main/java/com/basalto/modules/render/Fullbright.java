package com.basalto.modules.render;

import com.basalto.module.Category;
import com.basalto.module.Module;

/** Aplicado pelo GameRendererMixin (força a visão noturna no máximo). */
public class Fullbright extends Module {
	public Fullbright() {
		super("Fullbright", Category.RENDER, "Deixa tudo claro, até cavernas.");
	}
}
