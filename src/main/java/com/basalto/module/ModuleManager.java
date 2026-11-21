package com.basalto.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.basalto.modules.combat.*;
import com.basalto.modules.movement.*;
import com.basalto.modules.player.*;
import com.basalto.modules.render.*;
import com.basalto.modules.world.*;

public final class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();

	public ModuleManager() {
		// Combat
		register(new KillAura());
		register(new TriggerBot());
		register(new AutoClicker());
		register(new AimAssist());
		register(new Reach());
		register(new Velocity());
		register(new AutoTotem());
		// Movement
		register(new Fly());
		register(new Speed());
		register(new Sprint());
		register(new NoFall());
		register(new Step());
		register(new Spider());
		register(new Glide());
		register(new SafeWalk());
		register(new Parkour());
		register(new AutoWalk());
		register(new BoatFly());
		register(new NoSlow());
		// Render
		register(new ESP());
		register(new Fullbright());
		register(new XRay());
		register(new Zoom());
		register(new NoHurtCam());
		register(new NoWeather());
		// Player
		register(new AutoEat());
		register(new AutoTool());
		register(new FastPlace());
		register(new FastBreak());
		register(new ChestStealer());
		register(new AutoRespawn());
		register(new AntiAFK());
		// World
		register(new Timer());
		register(new Nuker());
		register(new Scaffold());
	}

	private void register(Module m) {
		modules.add(m);
		byClass.put(m.getClass(), m);
	}

	public List<Module> all() {
		return Collections.unmodifiableList(modules);
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> type) {
		return (T) byClass.get(type);
	}

	public Module find(String name) {
		for (Module m : modules)
			if (m.name.equalsIgnoreCase(name)) return m;
		return null;
	}

	public List<Module> inCategory(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : modules)
			if (m.category == c) out.add(m);
		return out;
	}

	public static boolean on(Class<? extends Module> type) {
		ModuleManager mm = com.basalto.Basalto.modules();
		if (mm == null) return false;
		Module m = mm.get(type);
		return m != null && m.isEnabled();
	}
}
