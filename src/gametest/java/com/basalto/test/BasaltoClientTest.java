package com.basalto.test;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.MixinEnvironment;

import com.basalto.Basalto;
import com.basalto.gui.App;
import com.basalto.gui.ClickGuiScreen;
import com.basalto.gui.Modal;
import com.basalto.module.Module;
import com.basalto.modules.combat.KillAura;
import com.basalto.modules.movement.Fly;
import com.basalto.modules.movement.Sprint;
import com.basalto.modules.render.ESP;
import com.basalto.modules.render.Fullbright;
import com.basalto.modules.render.XRay;
import com.basalto.modules.render.Zoom;

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

			// 2. Menu no Right Shift
			ctx.getInput().resizeWindow(1920, 1080);
			ctx.waitTicks(3);
			ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> mc.screen instanceof ClickGuiScreen), "Right Shift abre o menu");
			menuTour(ctx, world);
			ctx.setScreen(() -> null);
			ctx.getInput().resizeWindow(854, 480);
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
			ctx.getInput().resizeWindow(1920, 1080);
			ctx.runOnClient(mc -> Basalto.config().client.hud.put("keystrokes", true));
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

	private static App app(Minecraft mc) {
		return ((ClickGuiScreen) mc.screen).app;
	}

	private void shot(ClientGameTestContext ctx, String name) {
		ctx.waitTicks(10);
		ctx.takeScreenshot(name);
	}

	/** Passa por todas as telas do menu, tira prints e testa perfis e teclas. */
	private void menuTour(ClientGameTestContext ctx, TestSingleplayerContext world) {
		ctx.runOnClient(mc -> {
			Basalto.modules().get(Sprint.class).setEnabled(true);
			Basalto.modules().get(Fullbright.class).setEnabled(true);
			Basalto.config().client.favorites.add("Fly");
			Basalto.config().client.favorites.add("Sprint");
			Basalto.config().client.favorites.add("ESP");
		});
		shot(ctx, "ui-01-painel");
		ctx.runOnClient(mc -> app(mc).navigate(App.Page.MODULES));
		shot(ctx, "ui-02-modulos");
		ctx.runOnClient(mc -> app(mc).modules.openDrawer(Basalto.modules().find("KillAura")));
		shot(ctx, "ui-03-modulo-opcoes");
		ctx.runOnClient(mc -> app(mc).modules.closeDrawer());
		ctx.getInput().typeChars("zzz");
		ctx.runOnClient(mc -> app(mc).ui.focus = "field:search");
		ctx.getInput().typeChars("qqq");
		shot(ctx, "ui-04-modulos-vazio");
		ctx.runOnClient(mc -> app(mc).ui.focus = "field:search");
		for (int i = 0; i < 6; i++) ctx.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);
		ctx.runOnClient(mc -> app(mc).navigate(App.Page.PROFILES));
		shot(ctx, "ui-05-perfis");
		ctx.runOnClient(mc -> app(mc).navigate(App.Page.HUD));
		shot(ctx, "ui-06-hud");
		String[] tabs = {"geral", "aparencia", "interface", "desempenho", "audio", "atalhos", "privacidade", "sobre"};
		for (int i = 0; i < tabs.length; i++) {
			String tab = tabs[i];
			ctx.runOnClient(mc -> {
				app(mc).settings.open(tab);
				app(mc).navigate(App.Page.SETTINGS);
			});
			shot(ctx, "ui-07-config-" + (i + 1) + "-" + tab);
		}

		// Modal de confirmação
		ctx.runOnClient(mc -> app(mc).openModal(Modal.confirm("trash-2", "Excluir \"PvP\"?",
			"O perfil some da lista. Exporte antes se quiser guardar uma cópia. Isso não pode ser desfeito.", "Excluir", true, () -> {})));
		shot(ctx, "ui-08-modal");
		ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		ctx.waitTicks(5);
		check(ctx.computeOnClient(mc -> mc.screen instanceof ClickGuiScreen && app(mc).modal == null), "Esc fecha só o modal");

		// Sino com atividade + toast
		ctx.runOnClient(mc -> {
			app(mc).navigate(App.Page.DASHBOARD);
			app(mc).showPanels(true, false);
			com.basalto.gui.Toasts.success("Perfil salvo", "Padrão");
		});
		shot(ctx, "ui-08b-sino-toast");
		ctx.runOnClient(mc -> app(mc).showPanels(false, true));
		shot(ctx, "ui-08c-menu-conta");
		ctx.runOnClient(mc -> app(mc).showPanels(false, false));

		// Tema claro, barra recolhida
		ctx.runOnClient(mc -> {
			Basalto.config().client.darkTheme = false;
			app(mc).navigate(App.Page.DASHBOARD);
		});
		shot(ctx, "ui-09-painel-claro");
		ctx.runOnClient(mc -> app(mc).navigate(App.Page.MODULES));
		shot(ctx, "ui-10-modulos-claro");
		ctx.runOnClient(mc -> {
			Basalto.config().client.darkTheme = true;
			Basalto.config().client.sidebarCollapsed = true;
		});
		shot(ctx, "ui-11-recolhida");
		ctx.runOnClient(mc -> Basalto.config().client.sidebarCollapsed = false);

		// Janela pequena
		ctx.getInput().resizeWindow(1280, 720);
		ctx.runOnClient(mc -> app(mc).navigate(App.Page.DASHBOARD));
		shot(ctx, "ui-12-painel-720p");
		ctx.getInput().resizeWindow(1024, 600);
		ctx.runOnClient(mc -> app(mc).navigate(App.Page.MODULES));
		shot(ctx, "ui-13-modulos-1024");
		ctx.getInput().resizeWindow(1920, 1080);
		ctx.waitTicks(3);

		// Perfis: criar, detectar mudança, renomear, exportar, importar, excluir
		ctx.runOnClient(mc -> {
			var cfg = Basalto.config();
			cfg.createProfile("Teste");
			check(cfg.profiles.containsKey("Teste") && cfg.activeProfile.equals("Teste"), "Cria e ativa um perfil");
			check(!cfg.activeProfileChanged(100000), "Perfil recém-salvo não tem alterações");
			Basalto.modules().get(ESP.class).toggle();
			check(cfg.activeProfileChanged(200000), "Detecta alteração não salva");
			cfg.renameProfile("Teste", "Teste 2");
			check(cfg.profiles.containsKey("Teste 2") && cfg.activeProfile.equals("Teste 2"), "Renomeia o perfil ativo");
			check(cfg.validateName("teste 2", null) != null, "Não aceita nome repetido");
			try {
				var f = cfg.exportProfile("Teste 2");
				String imported = cfg.importProfile(f);
				check(imported.equals("Teste 3") || imported.startsWith("Teste 2"), "Importa o perfil exportado (" + imported + ")");
				cfg.deleteProfile(imported);
				java.nio.file.Files.deleteIfExists(f);
			} catch (Exception e) {
				check(false, "Exporta e importa: " + e);
			}
			String dup = cfg.duplicateProfile("Teste 2");
			check(dup != null && cfg.profiles.containsKey(dup), "Duplica o perfil");
			cfg.deleteProfile(dup);
			cfg.deleteProfile("Teste 2");
			check(!cfg.profiles.containsKey("Teste 2"), "Exclui o perfil");
			Basalto.modules().get(ESP.class).setEnabled(false, false);
		});

		// Esc fecha o menu
		ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		ctx.waitTicks(10);
		check(ctx.computeOnClient(mc -> mc.screen == null), "Esc fecha o menu");

		// Tecla no modo segurar
		ctx.runOnClient(mc -> {
			var m = Basalto.modules().get(Zoom.class);
			m.key = GLFW.GLFW_KEY_C;
			m.holdKey = true;
			m.setEnabled(false, false);
		});
		ctx.getInput().holdKey(GLFW.GLFW_KEY_C);
		ctx.waitTicks(2);
		check(ctx.computeOnClient(mc -> Basalto.modules().get(Zoom.class).isEnabled()), "Segurar a tecla liga o módulo");
		ctx.getInput().releaseKey(GLFW.GLFW_KEY_C);
		ctx.waitTicks(2);
		check(ctx.computeOnClient(mc -> !Basalto.modules().get(Zoom.class).isEnabled()), "Soltar a tecla desliga o módulo");
		ctx.runOnClient(mc -> Basalto.modules().get(Zoom.class).holdKey = false);

		ctx.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
		ctx.waitTicks(5);
	}

	private static Zombie findZombie(Minecraft mc) {
		for (Entity e : mc.level.entitiesForRendering())
			if (e instanceof Zombie z) return z;
		return null;
	}
}
