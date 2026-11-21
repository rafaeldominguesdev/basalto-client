package com.basalto.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.basalto.Basalto;
import com.basalto.config.ClientSettings;
import com.basalto.gui.Draw;
import com.basalto.module.Module;
import com.basalto.modules.combat.KillAura;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Tudo que o client desenha na tela durante o jogo. */
public final class Hud {
	private static int frames;
	private static int fps;
	private static long fpsWindowStart = System.currentTimeMillis();

	private Hud() {}

	public static void render(GuiGraphicsExtractor g, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		countFps();
		if (mc.player == null || mc.options.hideGui) return;
		ClientSettings cs = Basalto.config().client;
		Font f = mc.font;
		int w = g.guiWidth();
		int h = g.guiHeight();
		int bg = cs.hudBgColor();

		int leftY = 4;
		if (cs.hud("watermark")) {
			String fpsText = cs.hud("fps") ? fps + " FPS" : "";
			int bw = 18 + f.width(Basalto.NAME) + (fpsText.isEmpty() ? 4 : 8 + f.width(fpsText)) + 4;
			g.fill(4, leftY, 4 + bw, leftY + 16, bg);
			Draw.cube(g, 6, leftY + 2, 12, cs.accent);
			g.text(f, Basalto.NAME, 22, leftY + 4, 0xFFF3F4F6, true);
			if (!fpsText.isEmpty()) g.text(f, fpsText, 22 + f.width(Basalto.NAME) + 8, leftY + 4, 0xFFD1D5DB, true);
			leftY += 20;
		} else if (cs.hud("fps")) {
			g.text(f, fps + " FPS", 6, leftY, 0xFFD1D5DB, true);
			leftY += 12;
		}

		if (cs.hud("coords")) leftY = drawCoords(g, f, mc.player, leftY, bg) + 4;
		if (cs.hud("potions")) leftY = drawPotions(g, f, mc.player, leftY, bg) + 4;
		if (cs.hud("keystrokes")) drawKeystrokes(g, f, mc, 6, h / 2 - 20, bg, cs.accent);
		if (cs.hud("arraylist")) drawArrayList(g, f, w, bg, cs.accent);
		if (cs.hud("armor")) drawArmor(g, f, mc.player, w, h);
		if (cs.hud("target")) drawTarget(g, f, mc, w, h, bg, cs.accent);
		if (cs.hud("notifications")) drawNotifications(g, f, w, h, bg, cs.accent);
	}

	private static void countFps() {
		frames++;
		long now = System.currentTimeMillis();
		if (now - fpsWindowStart >= 1000) {
			fps = frames;
			frames = 0;
			fpsWindowStart = now;
		}
	}

	private static int drawCoords(GuiGraphicsExtractor g, Font f, LocalPlayer p, int y, int bg) {
		List<String> lines = new ArrayList<>();
		lines.add(String.format("XYZ  %.0f / %.0f / %.0f", p.getX(), p.getY(), p.getZ()));
		lines.add(String.format("÷8  %.0f / %.0f   ×8  %.0f / %.0f", p.getX() / 8, p.getZ() / 8, p.getX() * 8, p.getZ() * 8));
		Direction d = p.getDirection();
		lines.add("Direção  " + directionName(d));
		return box(g, f, lines, 4, y, bg);
	}

	private static String directionName(Direction d) {
		return switch (d) {
			case NORTH -> "Norte (-Z)";
			case SOUTH -> "Sul (+Z)";
			case EAST -> "Leste (+X)";
			case WEST -> "Oeste (-X)";
			default -> d.getName();
		};
	}

	private static int drawPotions(GuiGraphicsExtractor g, Font f, LocalPlayer p, int y, int bg) {
		List<String> lines = new ArrayList<>();
		for (MobEffectInstance e : p.getActiveEffects()) {
			String name = e.getEffect().value().getDisplayName().getString();
			if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
			String time;
			if (e.getDuration() < 0) time = "∞";
			else {
				int s = e.getDuration() / 20;
				time = (s / 60) + ":" + String.format("%02d", s % 60);
			}
			lines.add(name + "  " + time);
		}
		if (lines.isEmpty()) return y - 4;
		return box(g, f, lines, 4, y, bg);
	}

	/** Caixa com várias linhas de texto; devolve o y do fim. */
	private static int box(GuiGraphicsExtractor g, Font f, List<String> lines, int x, int y, int bg) {
		int bw = 0;
		for (String s : lines) bw = Math.max(bw, f.width(s));
		int bh = lines.size() * 10 + 4;
		g.fill(x, y, x + bw + 8, y + bh, bg);
		for (int i = 0; i < lines.size(); i++) g.text(f, lines.get(i), x + 4, y + 3 + i * 10, 0xFFE5E7EB, true);
		return y + bh;
	}

	private static void drawArrayList(GuiGraphicsExtractor g, Font f, int w, int bg, int accent) {
		List<Module> active = new ArrayList<>();
		for (Module m : Basalto.modules().all()) if (m.isEnabled() && m.showInHud) active.add(m);
		active.sort(Comparator.comparingInt((Module m) -> f.width(label(m))).reversed());
		int y = 2;
		for (Module m : active) {
			String name = m.name;
			String info = m.getInfo();
			int tw = f.width(label(m));
			int x = w - tw - 9;
			g.fill(x - 4, y, w, y + 11, bg);
			g.fill(w - 2, y, w, y + 11, accent);
			g.text(f, name, x, y + 2, 0xFFF3F4F6, true);
			if (!info.isEmpty()) g.text(f, info, x + f.width(name + " "), y + 2, 0xFF9CA3AF, true);
			y += 11;
		}
	}

	private static String label(Module m) {
		String info = m.getInfo();
		return info.isEmpty() ? m.name : m.name + " " + info;
	}

	private static void drawArmor(GuiGraphicsExtractor g, Font f, LocalPlayer p, int w, int h) {
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		int x = w / 2 + 96;
		int y = h - 20;
		for (EquipmentSlot slot : slots) {
			ItemStack s = p.getItemBySlot(slot);
			if (s.isEmpty()) continue;
			g.item(s, x, y);
			if (s.getMaxDamage() > 0) {
				int pct = Math.round(100f * (s.getMaxDamage() - s.getDamageValue()) / s.getMaxDamage());
				int col = pct > 50 ? 0xFF4ADE80 : pct > 20 ? 0xFFF59E0B : 0xFFF87171;
				g.text(f, pct + "%", x + 1, y - 8, col, true);
			}
			x += 20;
		}
	}

	private static void drawKeystrokes(GuiGraphicsExtractor g, Font f, Minecraft mc, int x, int y, int bg, int accent) {
		int s = 16, gap = 2;
		key(g, f, "W", mc.options.keyUp.isDown(), x + s + gap, y, s, bg, accent);
		key(g, f, "A", mc.options.keyLeft.isDown(), x, y + s + gap, s, bg, accent);
		key(g, f, "S", mc.options.keyDown.isDown(), x + s + gap, y + s + gap, s, bg, accent);
		key(g, f, "D", mc.options.keyRight.isDown(), x + 2 * (s + gap), y + s + gap, s, bg, accent);
	}

	private static void key(GuiGraphicsExtractor g, Font f, String k, boolean down, int x, int y, int s, int bg, int accent) {
		g.fill(x, y, x + s, y + s, down ? accent : bg);
		g.text(f, k, x + (s - f.width(k)) / 2, y + 4, down ? 0xFF06140A : 0xFFE5E7EB, false);
	}

	private static void drawTarget(GuiGraphicsExtractor g, Font f, Minecraft mc, int w, int h, int bg, int accent) {
		LivingEntity target = null;
		KillAura ka = Basalto.modules().get(KillAura.class);
		if (ka != null && ka.isEnabled() && ka.target != null && ka.target.isAlive()) target = ka.target;
		if (target == null && mc.hitResult instanceof EntityHitResult ehr) {
			Entity e = ehr.getEntity();
			if (e instanceof LivingEntity le) target = le;
		}
		if (target == null) return;
		int x = w / 2 + 14;
		int y = h / 2 + 10;
		String name = target.getName().getString();
		float hp = target.getHealth();
		float max = Math.max(1, target.getMaxHealth());
		String hpText = String.format("%.1f / %.0f", hp, max);
		double dist = mc.player.distanceTo(target);
		int bw = Math.max(110, Math.max(f.width(name) + 40, f.width(hpText) + 8));
		g.fill(x, y, x + bw, y + 30, bg);
		g.text(f, name, x + 4, y + 3, 0xFFF3F4F6, true);
		String d = String.format("%.1fm", dist);
		g.text(f, d, x + bw - 4 - f.width(d), y + 3, 0xFF9CA3AF, true);
		g.fill(x + 4, y + 14, x + bw - 4, y + 17, 0xFF262A33);
		g.fill(x + 4, y + 14, x + 4 + Math.round((bw - 8) * Math.min(1, hp / max)), y + 17, accent);
		g.text(f, hpText, x + 4, y + 20, 0xFFD1D5DB, true);
	}

	private static void drawNotifications(GuiGraphicsExtractor g, Font f, int w, int h, int bg, int accent) {
		List<Notifications.Entry> list = Notifications.active();
		int y = h - 40;
		long now = System.currentTimeMillis();
		for (int i = list.size() - 1; i >= 0; i--) {
			Notifications.Entry e = list.get(i);
			String tag = e.on() ? "ON" : "OFF";
			int bw = f.width(e.text()) + f.width(tag) + 18;
			float age = (now - e.time()) / (float) Notifications.DURATION_MS;
			int slide = age < 0.08f ? Math.round((1 - age / 0.08f) * (bw + 6)) : 0;
			int x = w - bw - 6 + slide;
			g.fill(x, y - 16, x + bw, y, bg);
			g.fill(x, y - 16, x + 2, y, e.on() ? accent : 0xFF6B7280);
			g.text(f, e.text(), x + 7, y - 12, 0xFFE5E7EB, true);
			g.text(f, tag, x + bw - 5 - f.width(tag), y - 12, e.on() ? accent : 0xFF9CA3AF, true);
			y -= 19;
		}
	}
}
