package com.basalto.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.world.Timer;

import net.minecraft.client.DeltaTracker;

@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {
	@Shadow
	public float deltaTicks;

	@Inject(method = "advanceGameTime(J)I",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J", opcode = Opcodes.PUTFIELD, ordinal = 0))
	private void basalto$timer(long timeMillis, CallbackInfoReturnable<Integer> cir) {
		if (ModuleManager.on(Timer.class)) deltaTicks *= Basalto.modules().get(Timer.class).multiplier();
	}
}
