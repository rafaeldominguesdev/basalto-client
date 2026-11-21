package com.basalto.modules.render;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

/** Aplicado pelo CameraMixin (calculateFov). */
public class Zoom extends Module {
	private final NumberSetting factor = add(new NumberSetting("Zoom", 4, 1.5, 15, 0.5, "x"));

	public Zoom() {
		super("Zoom", Category.RENDER, "Aproxima a câmera como uma luneta.");
	}

	public float apply(float fov) {
		return (float) (fov / factor.getD());
	}

	@Override
	public String getInfo() {
		return String.format(java.util.Locale.ROOT, "%.1fx", factor.getD());
	}
}
