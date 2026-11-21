package com.basalto.hud;

import java.util.ArrayList;
import java.util.List;

import com.basalto.Basalto;

/** Histórico da sessão (módulos ligados, perfis carregados...). Aparece no Painel e no sino. */
public final class Activity {
	public enum Kind { ON, OFF, PROFILE, INFO, WARNING }

	public record Entry(long time, Kind kind, String icon, String text) {}

	private static final int MAX = 60;
	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static int unread;

	private Activity() {}

	public static synchronized void log(Kind kind, String icon, String text) {
		var cfg = Basalto.config();
		if (cfg != null && !cfg.client.keepActivity) return;
		ENTRIES.add(0, new Entry(System.currentTimeMillis(), kind, icon, text));
		while (ENTRIES.size() > MAX) ENTRIES.remove(ENTRIES.size() - 1);
		unread = Math.min(99, unread + 1);
	}

	public static synchronized List<Entry> recent(int max) {
		return new ArrayList<>(ENTRIES.subList(0, Math.min(max, ENTRIES.size())));
	}

	public static synchronized int size() {
		return ENTRIES.size();
	}

	public static synchronized int unread() {
		return unread;
	}

	public static synchronized void markRead() {
		unread = 0;
	}

	public static synchronized void clear() {
		ENTRIES.clear();
		unread = 0;
	}

	/** "agora", "há 3 min", "há 2 h". */
	public static String ago(long time) {
		long s = (System.currentTimeMillis() - time) / 1000;
		if (s < 45) return "agora";
		if (s < 3600) return "há " + Math.max(1, s / 60) + " min";
		if (s < 86400) return "há " + s / 3600 + " h";
		return "há " + s / 86400 + " d";
	}
}
