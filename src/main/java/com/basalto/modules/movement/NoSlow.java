package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;

/** Aplicado pelo LocalPlayerMixin (isSlowDueToUsingItem). */
public class NoSlow extends Module {
	public NoSlow() {
		super("NoSlow", Category.MOVEMENT, "Não fica lento comendo, usando escudo ou arco.");
	}
}
