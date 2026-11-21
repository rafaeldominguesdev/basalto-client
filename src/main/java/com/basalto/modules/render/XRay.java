package com.basalto.modules.render;

import java.util.Set;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Esconde os blocos comuns e mostra só minérios e blocos úteis. */
public class XRay extends Module {
	private final BoolSetting chests = add(new BoolSetting("Baús e spawners", true));
	private final BoolSetting liquids = add(new BoolSetting("Lava e água", true));

	private static final Set<String> EXTRA = Set.of("ancient_debris", "spawner", "trial_spawner", "vault", "chest",
		"trapped_chest", "barrel", "ender_chest", "shulker_box", "end_portal_frame", "budding_amethyst", "amethyst_cluster");
	private static final Set<String> FLUIDS = Set.of("lava", "water");

	public XRay() {
		super("XRay", Category.RENDER, "Vê minérios através das pedras.");
	}

	@Override
	public void onEnable() {
		reload();
	}

	@Override
	public void onDisable() {
		reload();
	}

	private void reload() {
		if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged();
	}

	public boolean isVisible(Block block) {
		String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
		if (id.endsWith("_ore") || id.equals("ancient_debris")) return true;
		if (chests.isOn() && (EXTRA.contains(id) || id.endsWith("shulker_box"))) return true;
		return liquids.isOn() && FLUIDS.contains(id);
	}

	/** true = desenhar a face, false = esconder, null = deixar o jogo decidir. */
	public Boolean shouldDrawSide(BlockState state) {
		return isVisible(state.getBlock());
	}
}
