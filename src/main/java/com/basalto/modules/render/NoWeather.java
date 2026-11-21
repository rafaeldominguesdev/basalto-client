package com.basalto.modules.render;

import com.basalto.module.Category;
import com.basalto.module.Module;

/** Aplicado pelo LevelMixin (getRainLevel). */
public class NoWeather extends Module {
	public NoWeather() {
		super("NoWeather", Category.RENDER, "Esconde chuva e neve.");
	}
}
