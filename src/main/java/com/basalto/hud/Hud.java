package com.basalto.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.basalto.Basalto;
import com.basalto.config.ClientSettings;
import com.basalto.gui.ClickGuiScreen;
import com.basalto.gui.Draw;
import com.basalto.gui.ModuleIcons;
import com.basalto.module.Module;
import com.basalto.modules.combat.KillAura;
import com.basalto.ui.Anim;
import com.basalto.ui.Colors;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;
import com.basalto.ui.Ui;

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

/** Tudo que o client desenha na tela durante o jogo, no mesmo estilo do menu. */
public final class Hud {
	private static final Ui UI = new Ui();
	private static final float M = 6;

	private Hud() {}

	public static void render(GuiGraphicsExtractor g, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.screen instanceof ClickGuiScreen) return;
		ClientSettings cs = Basalto.config().client;

		// Armadura fica em coordenadas do jogo (itens 3D).
		if (cs.hud("armor")) drawArmor(g, mc.font, mc.player, g.guiWidth(), g.guiHeight());

		Anim.motion = cs.motion;
		UI.begin(g, cs.theme(), Ui.autoDensity(mc.getWindow().getWidth(), mc.getWindow().getHeight(), cs.uiSize));
		Theme t = UI.t;
		int bg = cs.hudBgColor();

		float leftY = M;
		if (cs.hud("watermark")) leftY = watermark(t, bg, cs.hud("fps"), leftY) + 4;
		else if (cs.hud("fps")) {
			UI.text(Type.BODY_SEMI, mc.getFps() + " FPS", M + 2, leftY, t.text);
			leftY += 14;
		}
		if (cs.hud("coords") && !cs.streamerMode) leftY = coords(t, bg, mc.player, leftY) + 4;
		if (cs.hud("potions")) leftY = potions(t, bg, mc.player, leftY) + 4;
		if (cs.hud("keystrokes")) keystrokes(t, bg, mc, M, UI.H / 2 - 20);
		if (cs.hud("arraylist")) arrayList(t, bg);
		if (cs.hud("target")) target(t, bg, mc);
		if (cs.hud("notifications")) notifications(t, bg);
		UI.end();
	}

	// ---------------------------------------------------------------- logo + FPS

	private static float watermark(Theme t, int bg, boolean fps, float y) {
		String name = "Basalto";
		String f = Minecraft.getInstance().getFps() + " FPS";
		float w = 10 + 12 + 5 + UI.width(Type.BRAND, name) + (fps ? 14 + UI.width(Type.LABEL, f) : 0) + 10;
		float h = 20;
		UI.shadow(M, y, w, h, 8, 2, 0.25f);
		UI.round(M, y, w, h, Theme.R_LG, bg);
		UI.ring(M, y, w, h, Theme.R_LG, t.border);
		Draw.cube(UI.g, UI.px(M + 8), UI.px(y + 4), Math.round(12 * UI.p), t.accent);
		float x = M + 25;
		UI.textMid(Type.BRAND, name, x, y, h, t.text);
		if (fps) {
			x += UI.width(Type.BRAND, name) + 7;
			UI.vline(x, y + 5, h - 10, t.borderStrong);
			UI.textMid(Type.LABEL, f, x + 7, y, h, t.text2);
		}
		return y + h;
	}

	// ---------------------------------------------------------------- caixas de linhas

	private static float rows(Theme t, int bg, List<String[]> rows, float y) {
		if (rows.isEmpty()) return y - 4;
		float kw = 0, vw = 0;
		for (String[] r : rows) {
			kw = Math.max(kw, UI.width(Type.CAPTION, r[0]));
			vw = Math.max(vw, UI.width(Type.BODY_MEDIUM, r[1]));
		}
		float w = 10 + kw + 10 + vw + 10;
		float rh = 12;
		float h = rows.size() * rh + 8;
		UI.shadow(M, y, w, h, 8, 2, 0.25f);
		UI.round(M, y, w, h, Theme.R_LG, bg);
		UI.ring(M, y, w, h, Theme.R_LG, t.border);
		float ry = y + 4;
		for (String[] r : rows) {
			UI.textMid(Type.CAPTION, r[0], M + 10, ry, rh, t.text3);
			UI.textMid(Type.BODY_MEDIUM, r[1], M + 20 + kw, ry, rh, r.length > 2 ? t.accentText : t.text);
			ry += rh;
		}
		return y + h;
	}

	private static float coords(Theme t, int bg, LocalPlayer p, float y) {
		List<String[]> rows = new ArrayList<>();
		rows.add(new String[] {"XYZ", String.format("%.0f, %.0f, %.0f", p.getX(), p.getY(), p.getZ())});
		boolean nether = p.level().dimension().identifier().getPath().equals("the_nether");
		if (nether) rows.add(new String[] {"Superfície", String.format("%.0f, %.0f", p.getX() * 8, p.getZ() * 8)});
		else rows.add(new String[] {"Nether", String.format("%.0f, %.0f", p.getX() / 8, p.getZ() / 8)});
		rows.add(new String[] {"Direção", directionName(p.getDirection())});
		return rows(t, bg, rows, y);
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

	private static float potions(Theme t, int bg, LocalPlayer p, float y) {
		List<String[]> rows = new ArrayList<>();
		for (MobEffectInstance e : p.getActiveEffects()) {
			String name = e.getEffect().value().getDisplayName().getString();
			if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
			String time;
			if (e.getDuration() < 0) time = "∞";
			else {
				int s = e.getDuration() / 20;
				time = (s / 60) + ":" + String.format("%02d", s % 60);
			}
			rows.add(new String[] {time, name});
		}
		return rows(t, bg, rows, y);
	}

	// ---------------------------------------------------------------- lista de módulos

	private static void arrayList(Theme t, int bg) {
		List<Module> active = new ArrayList<>();
		for (Module m : Basalto.modules().all()) if (m.isEnabled() && m.showInHud) active.add(m);
		active.sort(Comparator.comparingDouble((Module m) -> width(m)).reversed());
		float y = M;
		float rh = 14;
		for (Module m : active) {
			float w = width(m) + 26;
			float x = UI.W - M - w;
			float k = UI.animFrom("hud:al:" + m.name, 0, 1, Theme.BASE);
			x += (1 - k) * 12 * Anim.move();
			UI.pushAlpha(k);
			UI.round(x, y, w, rh - 2, Theme.R_MD, bg);
			UI.icon(ModuleIcons.of(m.name), x + 6, y + 2.5f, 7, t.accentText);
			UI.textMid(Type.BODY_MEDIUM, m.name, x + 17, y, rh - 2, t.text);
			String info = m.getInfo();
			if (!info.isEmpty()) UI.textMid(Type.LABEL, info, x + 17 + UI.width(Type.BODY_MEDIUM, m.name) + 4, y, rh - 2, t.text3);
			UI.popAlpha();
			y += rh * k;
		}
	}

	private static float width(Module m) {
		String info = m.getInfo();
		return UI.width(Type.BODY_MEDIUM, m.name) + (info.isEmpty() ? 0 : 4 + UI.width(Type.LABEL, info));
	}

	// ---------------------------------------------------------------- teclas

	private static void keystrokes(Theme t, int bg, Minecraft mc, float x, float y) {
		float s = 18, gap = 2;
		key(t, bg, "W", mc.options.keyUp.isDown(), x + s + gap, y, s);
		key(t, bg, "A", mc.options.keyLeft.isDown(), x, y + s + gap, s);
		key(t, bg, "S", mc.options.keyDown.isDown(), x + s + gap, y + s + gap, s);
		key(t, bg, "D", mc.options.keyRight.isDown(), x + 2 * (s + gap), y + s + gap, s);
	}

	private static void key(Theme t, int bg, String k, boolean down, float x, float y, float s) {
		float a = UI.anim("hud:key:" + k, down ? 1 : 0, 80);
		UI.round(x, y, s, s, Theme.R_MD, Colors.mix(bg, t.accent, a));
		UI.ring(x, y, s, s, Theme.R_MD, Colors.mix(t.border, 0x00000000, a));
		UI.textCenter(Type.BODY_SEMI, k, x + s / 2, y + (s - Type.BODY_SEMI.lineHeight) / 2, Colors.mix(t.text, t.onAccent, a));
	}

	// ---------------------------------------------------------------- alvo

	private static void target(Theme t, int bg, Minecraft mc) {
		LivingEntity target = null;
		KillAura ka = Basalto.modules().get(KillAura.class);
		if (ka != null && ka.isEnabled() && ka.target != null && ka.target.isAlive()) target = ka.target;
		if (target == null && mc.hitResult instanceof EntityHitResult ehr) {
			Entity e = ehr.getEntity();
			if (e instanceof LivingEntity le) target = le;
		}
		if (target == null) return;
		float x = UI.W / 2 + 14;
		float y = UI.H / 2 + 10;
		String name = target.getName().getString();
		float hp = target.getHealth();
		float max = Math.max(1, target.getMaxHealth());
		String hpText = String.format("%.1f / %.0f", hp, max);
		String d = String.format("%.1f m", mc.player.distanceTo(target));
		float w = Math.max(120, UI.width(Type.BODY_SEMI, name) + UI.width(Type.LABEL, d) + 30);
		float h = 36;
		UI.shadow(x, y, w, h, 8, 2, 0.25f);
		UI.round(x, y, w, h, Theme.R_LG, bg);
		UI.ring(x, y, w, h, Theme.R_LG, t.border);
		UI.text(Type.BODY_SEMI, UI.fit(Type.BODY_SEMI, name, w - 50), x + 8, y + 5, t.text);
		UI.textRight(Type.LABEL, d, x + w - 8, y + 5, t.text2);
		float frac = Math.min(1, hp / max);
		int col = frac > 0.5f ? t.accent : frac > 0.25f ? t.warning : t.danger;
		UI.round(x + 8, y + 19, w - 16, 4, 2, t.dark ? 0xFF243032 : 0xFFD5DEDD);
		UI.round(x + 8, y + 19, Math.max(4, (w - 16) * frac), 4, 2, col);
		UI.text(Type.CAPTION, hpText, x + 8, y + 25, t.text2);
	}

	// ---------------------------------------------------------------- notificações

	private static void notifications(Theme t, int bg) {
		List<Notifications.Entry> list = Notifications.active();
		float y = UI.H - 44;
		long now = System.currentTimeMillis();
		for (int i = list.size() - 1; i >= 0; i--) {
			Notifications.Entry e = list.get(i);
			String name = e.text().split(" ")[0];
			String tag = e.on() ? "Ligado" : "Desligado";
			float w = Math.max(UI.width(Type.BODY_SEMI, name), UI.width(Type.CAPTION, tag)) + 40;
			float h = 26;
			float age = (now - e.time()) / (float) Notifications.DURATION_MS;
			float in = Math.min(1, age / 0.08f), out = age > 0.88f ? Math.max(0, (1 - age) / 0.12f) : 1;
			float k = Ui.ease(in) * out;
			float x = UI.W - M - w + (1 - Ui.ease(in)) * (w + M) * Anim.move();
			UI.pushAlpha(Anim.motion == Anim.Motion.FULL ? out : k);
			UI.shadow(x, y - h, w, h, 8, 2, 0.3f);
			UI.round(x, y - h, w, h, Theme.R_LG, bg);
			UI.ring(x, y - h, w, h, Theme.R_LG, e.on() ? t.accentBorder : t.border);
			UI.round(x + 7, y - h + 6, 14, 14, Theme.R_SM, e.on() ? t.accentFill : t.surface2);
			UI.icon(ModuleIcons.of(name), x + 10, y - h + 9, 8, e.on() ? t.accentText : t.text3);
			UI.text(Type.BODY_SEMI, name, x + 27, y - h + 3, t.text);
			UI.text(Type.CAPTION, tag, x + 27, y - h + 13.5f, e.on() ? t.accentText : t.text3);
			UI.popAlpha();
			y -= (h + 5) * k;
		}
	}

	// ---------------------------------------------------------------- armadura (coordenadas do jogo)

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
				int col = pct > 50 ? 0xFF54F5C4 : pct > 20 ? 0xFFE9C46A : 0xFFF2817A;
				g.text(f, pct + "%", x + 1, y - 8, col, true);
			}
			x += 20;
		}
	}
}
