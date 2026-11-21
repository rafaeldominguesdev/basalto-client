package com.basalto.gui;

import com.basalto.Basalto;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * O menu do client. Abre com a tecla do menu (padrão Right Shift).
 * A Screen só repassa entrada e desenho para o {@link App}.
 */
public final class ClickGuiScreen extends Screen {
	public final App app = new App();
	private boolean closed;

	public ClickGuiScreen() {
		super(Component.literal("Basalto"));
	}

	@Override
	public boolean isPauseScreen() {
		return Basalto.config().client.pauseGame;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		// O Esc fecha primeiro modais e painéis; o App decide.
		return false;
	}

	@Override
	public void onClose() {
		app.requestClose(this::finishClose);
	}

	private void finishClose() {
		if (closed) return;
		closed = true;
		Basalto.config().save();
		Minecraft.getInstance().setScreen(null);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (Basalto.config().client.blur) extractBlurredBackground(g);
		g.fill(0, 0, width, height, Basalto.config().client.darkTheme ? 0x66030607 : 0x40122220);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		app.draw(g);
		if (app.closeRequested) {
			app.closeRequested = false;
			onClose();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		app.ui.onMouse(event.button(), true);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		app.ui.onMouse(event.button(), false);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		return true;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		app.ui.onWheel(vertical);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		app.ui.onKey(event);
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		app.ui.onChar(event.codepoint());
		return true;
	}
}
