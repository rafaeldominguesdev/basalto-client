package com.basalto.modules.combat;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

/** O alcance é aplicado pelo PlayerMixin. */
public class Reach extends Module {
	public final NumberSetting distance = add(new NumberSetting("Distância", 5, 3, 7, 0.1, "m"));

	public Reach() {
		super("Reach", Category.COMBAT, "Aumenta o alcance para bater e colocar blocos.");
	}

	@Override
	public String getInfo() {
		return String.format(java.util.Locale.ROOT, "%.1f", distance.getD());
	}
}
