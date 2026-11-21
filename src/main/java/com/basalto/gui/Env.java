package com.basalto.gui;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

/** Lê o ambiente atual: tipo de mundo, nome, latência, ticks e memória. Só dado real. */
public final class Env {
	public enum Kind {
		LOCAL("Local", "house"), LAN("LAN", "network"), SERVER("Servidor", "server"), NONE("Sem mundo", "wifi-off");

		public final String label, icon;

		Kind(String label, String icon) {
			this.label = label;
			this.icon = icon;
		}
	}

	private static final Minecraft MC = Minecraft.getInstance();

	private Env() {}

	public static Kind kind() {
		if (MC.level == null || MC.player == null) return Kind.NONE;
		IntegratedServer sp = MC.getSingleplayerServer();
		if (sp != null) return sp.isPublished() ? Kind.LAN : Kind.LOCAL;
		ServerData sd = MC.getCurrentServer();
		if (sd != null && (sd.isLan() || isLocalAddress(sd.ip))) return Kind.LAN;
		return Kind.SERVER;
	}

	private static boolean isLocalAddress(String ip) {
		if (ip == null) return false;
		String h = ip.toLowerCase(Locale.ROOT).replaceFirst(":\\d+$", "");
		return h.equals("localhost") || h.startsWith("127.") || h.startsWith("192.168.") || h.startsWith("10.")
			|| h.matches("172\\.(1[6-9]|2\\d|3[01])\\..*");
	}

	/** Nome do mundo (singleplayer) ou do servidor. */
	public static String worldName() {
		IntegratedServer sp = MC.getSingleplayerServer();
		if (sp != null) return sp.getWorldData().getLevelName();
		ServerData sd = MC.getCurrentServer();
		if (sd != null) return sd.name;
		return MC.level != null ? "Mundo" : "—";
	}

	public static String address() {
		ServerData sd = MC.getCurrentServer();
		return sd == null ? null : sd.ip;
	}

	public static String dimension() {
		if (MC.level == null) return "—";
		String path = MC.level.dimension().identifier().getPath();
		return switch (path) {
			case "overworld" -> "Superfície";
			case "the_nether" -> "Nether";
			case "the_end" -> "The End";
			default -> path;
		};
	}

	/** Latência em ms (multiplayer), ou -1. */
	public static int ping() {
		if (MC.getSingleplayerServer() != null || MC.getConnection() == null || MC.player == null) return -1;
		PlayerInfo info = MC.getConnection().getPlayerInfo(MC.player.getUUID());
		return info == null ? -1 : info.getLatency();
	}

	/** Milissegundos por tick do servidor integrado, ou -1. */
	public static float mspt() {
		IntegratedServer sp = MC.getSingleplayerServer();
		if (sp == null) return -1;
		return sp.getAverageTickTimeNanos() / 1_000_000f;
	}

	public static float tps() {
		float ms = mspt();
		if (ms < 0) return -1;
		return Math.min(20f, 1000f / Math.max(50f, ms));
	}

	public static int fps() {
		return MC.getFps();
	}

	public static long memUsedMb() {
		Runtime r = Runtime.getRuntime();
		return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
	}

	public static long memMaxMb() {
		return Runtime.getRuntime().maxMemory() / (1024 * 1024);
	}

	public static String playerName() {
		return MC.player != null ? MC.player.getName().getString() : MC.getUser().getName();
	}

	/** Hora do dia no mundo, "14:32". */
	public static String worldClock() {
		if (MC.level == null) return "—";
		long t = (MC.level.getOverworldClockTime() + 6000) % 24000;
		int h = (int) (t / 1000), m = (int) ((t % 1000) * 60 / 1000);
		return String.format("%02d:%02d", h, m);
	}
}
