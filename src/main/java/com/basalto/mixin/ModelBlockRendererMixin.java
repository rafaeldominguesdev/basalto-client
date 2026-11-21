package com.basalto.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.basalto.Basalto;
import com.basalto.module.ModuleManager;
import com.basalto.modules.render.XRay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** XRay no renderizador vanilla (quando o Indigo não está desenhando o bloco). */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
	@WrapOperation(
		method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"),
		require = 0)
	private boolean basalto$xray(BlockState state, BlockState otherState, Direction side, Operation<Boolean> original,
		BlockAndTintGetter level, BlockState state2, Direction side2, BlockPos neighborPos) {
		if (ModuleManager.on(XRay.class)) {
			Boolean draw = Basalto.modules().get(XRay.class).shouldDrawSide(state);
			if (draw != null) return draw;
		}
		return original.call(state, otherState, side);
	}
}
