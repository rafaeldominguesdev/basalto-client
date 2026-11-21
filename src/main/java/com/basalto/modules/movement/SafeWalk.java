package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;

/** Aplicado pelo PlayerMixin (isStayingOnGroundSurface). */
public class SafeWalk extends Module {
	public SafeWalk() {
		super("SafeWalk", Category.MOVEMENT, "Não deixa cair da beirada dos blocos.");
	}
}
