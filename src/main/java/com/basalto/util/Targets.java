package com.basalto.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Encontrar e mirar em alvos (jogadores, mobs, animais). */
public final class Targets {
	private Targets() {}

	public static boolean isValid(Entity e, boolean players, boolean hostiles, boolean animals) {
		Minecraft mc = Minecraft.getInstance();
		if (!(e instanceof LivingEntity le) || e == mc.player) return false;
		if (!le.isAlive() || le.getHealth() <= 0) return false;
		if (e instanceof ArmorStand) return false;
		if (e instanceof Player) return players && !((Player) e).isSpectator();
		if (e instanceof Enemy) return hostiles;
		if (e instanceof Animal) return animals;
		return false;
	}

	public static List<LivingEntity> find(double range, boolean players, boolean hostiles, boolean animals) {
		Minecraft mc = Minecraft.getInstance();
		List<LivingEntity> out = new ArrayList<>();
		if (mc.level == null || mc.player == null) return out;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!isValid(e, players, hostiles, animals)) continue;
			if (mc.player.distanceTo(e) > range) continue;
			out.add((LivingEntity) e);
		}
		out.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
		return out;
	}

	/** Yaw e pitch para olhar para o centro do alvo. */
	public static float[] rotationsTo(Entity target) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 eyes = mc.player.getEyePosition();
		Vec3 aim = target.getBoundingBox().getCenter();
		double dx = aim.x - eyes.x;
		double dy = aim.y - eyes.y;
		double dz = aim.z - eyes.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
		return new float[] {Mth.wrapDegrees(yaw), Math.clamp(pitch, -90f, 90f)};
	}

	/** Gira suavemente a câmera do jogador em direção ao alvo (velocidade em graus por tick). */
	public static void lookAt(Entity target, float speed) {
		Minecraft mc = Minecraft.getInstance();
		float[] rot = rotationsTo(target);
		float yaw = mc.player.getYRot();
		float pitch = mc.player.getXRot();
		float dy = Mth.wrapDegrees(rot[0] - yaw);
		float dp = rot[1] - pitch;
		dy = Math.clamp(dy, -speed, speed);
		dp = Math.clamp(dp, -speed, speed);
		mc.player.setYRot(yaw + dy);
		mc.player.setXRot(Math.clamp(pitch + dp, -90f, 90f));
	}
}
