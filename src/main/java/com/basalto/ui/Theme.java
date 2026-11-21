package com.basalto.ui;

/**
 * Tokens de cor do design system. Duas paletas (escura e clara) a partir da
 * mesma cor de destaque. Espaçamento, raios e tempos de animação ficam aqui
 * também, como constantes, para todas as telas usarem os mesmos valores.
 */
public final class Theme {
	// Espaçamento (unidades da interface; 1 unidade = 2 px na densidade padrão)
	public static final float S1 = 2, S2 = 4, S3 = 6, S4 = 8, S5 = 12, S6 = 16, S7 = 20, S8 = 24, S9 = 32;
	// Raios
	public static final float R_SM = 4, R_MD = 6, R_LG = 8, R_XL = 10;
	// Alturas de controle
	public static final float CONTROL_H = 16, CONTROL_SM = 13;
	// Tempos de animação (ms)
	public static final float FAST = 150, BASE = 200, SLOW = 250;

	/** Cores de destaque oferecidas em Aparência. A primeira é a padrão. */
	public static final int[] ACCENTS = {0xFF54F5C4, 0xFF6CB6FF, 0xFFF2C14E, 0xFFFF8F7E, 0xFFF59AC7, 0xFFD5E1E0};
	public static final String[] ACCENT_NAMES = {"Verde-água", "Azul", "Âmbar", "Coral", "Rosa", "Gelo"};

	public final boolean dark;
	/** Fundo da janela, superfícies em três alturas e estados de interação. */
	public final int window, surface, surface2, elevated, hover, pressed, field;
	public final int border, borderStrong, divider;
	public final int text, text2, text3, onAccent;
	/** accent = preenchimento; accentText = ícones e texto em destaque sobre superfícies; accentFill = fundo translúcido. */
	public final int accent, accent2, accentSoft, accentText, accentFill, accentBorder;
	public final int danger, dangerFill, warning, warningFill, info, infoFill;
	public final int scrim, shadow, skeleton;

	private Theme(boolean dark, int accent) {
		this.dark = dark;
		this.accent = accent;
		this.accent2 = Colors.mix(accent, 0xFF000000, 0.15f);
		this.accentSoft = Colors.mix(accent, 0xFFFFFFFF, 0.45f);
		if (dark) {
			window = 0xFF0C1214;
			surface = 0xFF101719;
			surface2 = 0xFF172124;
			elevated = 0xFF1D2B2D;
			hover = 0x0FFFFFFF;
			pressed = 0x18FFFFFF;
			field = 0xFF0B1113;
			border = 0x14FFFFFF;
			borderStrong = 0x26FFFFFF;
			divider = 0x0FFFFFFF;
			text = 0xFFE6EDEC;
			text2 = 0xFF91A4A3;
			text3 = 0xFF627675;
			onAccent = 0xFF04201A;
			accentText = accent;
			accentFill = Colors.withAlpha(accent, 0.12f);
			accentBorder = Colors.withAlpha(accent, 0.38f);
			danger = 0xFFF2817A;
			dangerFill = 0x24F2817A;
			warning = 0xFFE9C46A;
			warningFill = 0x22E9C46A;
			info = 0xFF8FB8D8;
			infoFill = 0x1E8FB8D8;
			scrim = 0x99040809;
			shadow = 0xFF000000;
			skeleton = 0x14FFFFFF;
		} else {
			window = 0xFFEFF4F3;
			surface = 0xFFFFFFFF;
			surface2 = 0xFFF3F7F6;
			elevated = 0xFFFFFFFF;
			hover = 0x0A0B2B26;
			pressed = 0x140B2B26;
			field = 0xFFF6F9F8;
			border = 0x1A0B2B26;
			borderStrong = 0x2E0B2B26;
			divider = 0x120B2B26;
			text = 0xFF0F1B1C;
			text2 = 0xFF4F6564;
			text3 = 0xFF7D9190;
			onAccent = 0xFF04201A;
			accentText = Colors.mix(accent, 0xFF000000, 0.45f);
			accentFill = Colors.withAlpha(accent, 0.20f);
			accentBorder = Colors.withAlpha(Colors.mix(accent, 0xFF000000, 0.3f), 0.45f);
			danger = 0xFFC8433B;
			dangerFill = 0x1CC8433B;
			warning = 0xFFA97B0F;
			warningFill = 0x22D9A22B;
			info = 0xFF3F6E93;
			infoFill = 0x1A3F6E93;
			scrim = 0x660B1A1A;
			shadow = 0xFF0B2B26;
			skeleton = 0x120B2B26;
		}
	}

	public static Theme of(boolean dark, int accent) {
		return new Theme(dark, accent);
	}

	/** Fundo da janela com a opacidade escolhida (30 a 100%). */
	public int windowColor(int opacityPct) {
		return Colors.withAlpha(window, Math.max(0.3f, Math.min(1f, opacityPct / 100f)));
	}
}
