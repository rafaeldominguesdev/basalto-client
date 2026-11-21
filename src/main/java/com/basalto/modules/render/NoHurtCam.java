package com.basalto.modules.render;

import com.basalto.module.Category;
import com.basalto.module.Module;

/** Aplicado pelo GameRendererMixin (bobHurt). */
public class NoHurtCam extends Module {
	public NoHurtCam() {
		super("NoHurtCam", Category.RENDER, "Tira o tremor da câmera ao levar dano.");
	}
}
