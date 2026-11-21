package com.basalto.test;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.MixinEnvironment;

import com.basalto.Basalto;
import com.basalto.gui.ClickGuiScreen;
import com.basalto.module.Module;
import com.basalto.modules.combat.KillAura;
import com.basalto.modules.movement.Fly;
import com.basalto.modules.render.ESP;
import com.basalto.modules.render.Fullbright;
import com.basalto.modules.render.XRay;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * Abre um mundo, aperta Right Shift, liga Fly, ESP, Fullbright, XRay e KillAura
 * e tira um print de cada etapa (ficam em build/run/clientGameTest/screenshots).
 */
public class BasaltoClientTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();

	private void check(boolean ok, String what) {
		Basalto.LOGGER.info("[teste] {} {}", ok ? "OK  " : "FALHOU", what);
		if (!ok) failures.add(what);
	}

	private static void set(Class<? extends Module> type, boolean on) {
		Basalto.modules().get(type).setEnabled(on);
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		// Carrega todas as classes alvo dos mixins para os avisos de injeção aparecerem no log.
		ctx.runOnClient(mc -> MixinEnvironment.getCurrentEnvironment().audit());
		// Começa do zero, sem módulos ligados de uma rodada anterior.
		ctx.runOnClient(mc -> {
			for (Module m : Basalto.modules().all()) m.setEnabled(false, false);
		});

		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getClientLevel().waitForChunksRender();
			var server = world.getServer();
			server.runCommand("time set midnight");
			server.runCommand("weather clear");
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("gamemode survival @a");
			// Parede de pedra com minérios dentro, na frente do jogador (olhando para o sul).
			server.runCommand("tp @a 0.5 -60 0.5 0 0");
			server.runCommand("fill -4 -60 5 4 -55 7 stone");
			server.runCommand("setblock 0 -58 6 diamond_ore");
			server.runCommand("setblock 2 -57 6 gold_ore");
			server.runCommand("setblock -2 -59 6 iron_ore");
			ctx.waitTicks(20);
			world.getClientLevel().waitForChunksRender();

			// 1. HUD
			ctx.takeScreenshot("basalto-1-hud");

			// 2. ClickGUI no Right Shift
			ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> mc.screen instanceof ClickGuiScreen), "Right Shift abre o ClickGUI");
			ctx.takeScreenshot("basalto-2-clickgui");
			ctx.setScreen(() -> null);
			ctx.waitTicks(5);

			// 3. Fullbright
			ctx.runOnClient(mc -> set(Fullbright.class, true));
			ctx.waitTicks(10);
			ctx.takeScreenshot("basalto-3-fullbright");
			ctx.runOnClient(mc -> set(Fullbright.class, false));

			// 4. XRay
			ctx.runOnClient(mc -> set(XRay.class, true));
			ctx.waitTicks(10);
			world.getClientLevel().waitForChunksRender();
			ctx.takeScreenshot("basalto-4-xray");
			ctx.runOnClient(mc -> set(XRay.class, false));
			ctx.waitTicks(10);
			world.getClientLevel().waitForChunksRender();

			// 5. Fly: segurar espaço tem que subir sem cair de volta
			double y0 = ctx.computeOnClient(mc -> mc.player.getY());
			ctx.runOnClient(mc -> set(Fly.class, true));
			ctx.getInput().holdKeyFor(o -> o.keyJump, 15);
			ctx.waitTicks(20);
			double y1 = ctx.computeOnClient(mc -> mc.player.getY());
			check(y1 > y0 + 2, String.format("Fly sobe com espaço (y %.2f -> %.2f)", y0, y1));
			ctx.takeScreenshot("basalto-5-fly");
			ctx.runOnClient(mc -> set(Fly.class, false));
			server.runCommand("tp @a 0.5 -60 0.5 0 0");
			ctx.waitTicks(20);

			// 6. ESP e KillAura num zumbi parado ao lado
			server.runCommand("fill -4 -60 5 4 -55 7 air");
			server.runCommand("summon zombie 2.5 -60 2.5 {NoAI:1b,PersistenceRequired:1b,Health:200f,attributes:[{id:\"minecraft:max_health\",base:200}]}");
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> set(ESP.class, true));
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> {
				Zombie z = findZombie(mc);
				return z != null && mc.shouldEntityAppearGlowing(z);
			}), "ESP faz o zumbi brilhar");

			float hp0 = ctx.computeOnClient(mc -> {
				Zombie z = findZombie(mc);
				return z == null ? -1f : z.getHealth();
			});
			ctx.runOnClient(mc -> set(KillAura.class, true));
			ctx.waitTicks(60);
			check(ctx.computeOnClient(mc -> Basalto.modules().get(KillAura.class).target instanceof Zombie),
				"KillAura escolhe o zumbi como alvo");
			float hp1 = ctx.computeOnClient(mc -> {
				Zombie z = findZombie(mc);
				return z == null ? -1f : z.getHealth();
			});
			check(hp0 > 0 && hp1 < hp0, String.format("KillAura bate no zumbi (vida %.1f -> %.1f)", hp0, hp1));
			ctx.takeScreenshot("basalto-6-esp-killaura");

			// 7. HUD com tudo ligado de novo
			ctx.runOnClient(mc -> {
				set(Fullbright.class, true);
				set(Fly.class, true);
			});
			ctx.waitTicks(10);
			ctx.takeScreenshot("basalto-7-hud-modulos");
			ctx.runOnClient(mc -> {
				for (Module m : Basalto.modules().all()) m.setEnabled(false, false);
			});
		}

		if (!failures.isEmpty()) throw new AssertionError("Testes do Basalto falharam: " + failures);
	}

	private static Zombie findZombie(Minecraft mc) {
		for (Entity e : mc.level.entitiesForRendering())
			if (e instanceof Zombie z) return z;
		return null;
	}
}
