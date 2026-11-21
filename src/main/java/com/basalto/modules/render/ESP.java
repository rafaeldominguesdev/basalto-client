package com.basalto.modules.render;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;
import com.basalto.setting.ModeSetting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Contorno brilhante (glow) através das paredes. O brilho é ligado pelo
 * MinecraftMixin e a cor pelo EntityMixin.
 */
public class ESP extends Module {
	private final BoolSetting players = add(new BoolSetting("Jogadores", true));
	private final BoolSetting hostiles = add(new BoolSetting("Mobs hostis", true));
	private final BoolSetting animals = add(new BoolSetting("Animais", false));
	private final BoolSetting items = add(new BoolSetting("Itens no chão", false));
	private final ModeSetting colors = add(new ModeSetting("Cores", "Por tipo", "Por tipo", "Destaque"));

	public ESP() {
		super("ESP", Category.RENDER, "Mostra jogadores e mobs através das paredes.");
	}

	public boolean shouldGlow(Entity e) {
		if (e == mc.player) return false;
		if (e instanceof Player) return players.isOn();
		if (e instanceof Enemy) return hostiles.isOn();
		if (e instanceof Animal) return animals.isOn();
		if (e instanceof ItemEntity) return items.isOn();
		return false;
	}

	/** Cor RGB do contorno. */
	public int color(Entity e) {
		if (colors.is("Destaque")) return com.basalto.Basalto.config().client.accent & 0xFFFFFF;
		if (e instanceof Player) return 0x60A5FA;
		if (e instanceof Enemy) return 0xF87171;
		if (e instanceof Animal) return 0x4ADE80;
		if (e instanceof ItemEntity) return 0xF59E0B;
		return 0xFFFFFF;
	}
}
