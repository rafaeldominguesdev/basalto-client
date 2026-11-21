package com.basalto.gui;

import java.util.Map;

import com.basalto.module.Category;

/** Ícone e rótulo em português de cada módulo e categoria. */
public final class ModuleIcons {
	private static final Map<String, String> ICONS = Map.ofEntries(
		Map.entry("KillAura", "swords"), Map.entry("TriggerBot", "crosshair"), Map.entry("AutoClicker", "mouse-pointer-click"),
		Map.entry("AimAssist", "locate-fixed"), Map.entry("Reach", "ruler"), Map.entry("Velocity", "shield"),
		Map.entry("AutoTotem", "heart-pulse"), Map.entry("Fly", "bird"), Map.entry("Speed", "fast-forward"),
		Map.entry("Sprint", "footprints"), Map.entry("NoFall", "arrow-down-to-line"), Map.entry("Step", "chevrons-up"),
		Map.entry("Spider", "bug"), Map.entry("Glide", "feather"), Map.entry("SafeWalk", "shield-check"),
		Map.entry("Parkour", "person-standing"), Map.entry("AutoWalk", "navigation"), Map.entry("BoatFly", "sailboat"),
		Map.entry("NoSlow", "snail"), Map.entry("ESP", "scan-eye"), Map.entry("Fullbright", "lightbulb"),
		Map.entry("XRay", "gem"), Map.entry("Zoom", "zoom-in"), Map.entry("NoHurtCam", "camera"),
		Map.entry("NoWeather", "cloud-off"), Map.entry("AutoEat", "apple"), Map.entry("AutoTool", "pickaxe"),
		Map.entry("FastPlace", "square-plus"), Map.entry("FastBreak", "hammer"), Map.entry("ChestStealer", "package-open"),
		Map.entry("AutoRespawn", "refresh-ccw"), Map.entry("AntiAFK", "hourglass"), Map.entry("Timer", "timer"),
		Map.entry("Nuker", "bomb"), Map.entry("Scaffold", "brick-wall"));

	private ModuleIcons() {}

	public static String of(String module) {
		return ICONS.getOrDefault(module, "box");
	}

	public static String label(Category c) {
		return switch (c) {
			case COMBAT -> "Combate";
			case MOVEMENT -> "Movimento";
			case RENDER -> "Visual";
			case PLAYER -> "Jogador";
			case WORLD -> "Mundo";
		};
	}

	public static String icon(Category c) {
		return switch (c) {
			case COMBAT -> "swords";
			case MOVEMENT -> "move";
			case RENDER -> "eye";
			case PLAYER -> "user-round";
			case WORLD -> "globe";
		};
	}
}
