package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

/** A altura do passo é aplicada pelo LivingEntityMixin (maxUpStep). */
public class Step extends Module {
	public final NumberSetting height = add(new NumberSetting("Altura", 1, 1, 5, 0.5, " blocos"));

	public Step() {
		super("Step", Category.MOVEMENT, "Sobe blocos sem pular.");
	}

	@Override
	public String getInfo() {
		return String.format(java.util.Locale.ROOT, "%.1f", height.getD());
	}
}
