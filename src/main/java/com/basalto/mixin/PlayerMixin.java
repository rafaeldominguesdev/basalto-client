package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.combat.Reach;
import com.basalto.modules.movement.SafeWalk;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/** Reach: alcance maior para bater e interagir. */
	@ModifyReturnValue(method = {"blockInteractionRange()D", "entityInteractionRange()D"}, at = @At("RETURN"))
	private double basalto$reach(double original) {
		if ((Object) this != Minecraft.getInstance().player || !ModuleManager.on(Reach.class)) return original;
		return Basalto.modules().get(Reach.class).distance.getD();
	}

	/** SafeWalk: age como se estivesse agachado na beirada. */
	@ModifyReturnValue(method = "isStayingOnGroundSurface()Z", at = @At("RETURN"))
	private boolean basalto$safeWalk(boolean original) {
		if ((Object) this != Minecraft.getInstance().player) return original;
		return original || ModuleManager.on(SafeWalk.class);
	}
}
