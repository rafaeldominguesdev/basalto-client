package com.basalto.modules.world;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

/** Aplicado pelo DeltaTrackerTimerMixin: acelera ou desacelera o tempo do jogo no seu cliente. */
public class Timer extends Module {
	private final NumberSetting speed = add(new NumberSetting("Velocidade", 1.5, 0.1, 5, 0.1, "x"));

	public Timer() {
		super("Timer", Category.WORLD, "Acelera o tempo do jogo para você.");
	}

	public float multiplier() {
		return speed.getF();
	}

	@Override
	public String getInfo() {
		return String.format(java.util.Locale.ROOT, "%.1fx", speed.getD());
	}
}
