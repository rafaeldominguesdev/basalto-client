package com.basalto.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Avisos curtos no canto da tela ("Fly ativado"). */
public final class Notifications {
	public static final long DURATION_MS = 2200;
	private static final List<Entry> ENTRIES = new ArrayList<>();

	public record Entry(String text, boolean on, long time) {}

	private Notifications() {}

	public static synchronized void push(String text, boolean on) {
		ENTRIES.add(new Entry(text, on, System.currentTimeMillis()));
		while (ENTRIES.size() > 5) ENTRIES.remove(0);
	}

	public static synchronized List<Entry> active() {
		long now = System.currentTimeMillis();
		Iterator<Entry> it = ENTRIES.iterator();
		while (it.hasNext())
			if (now - it.next().time > DURATION_MS) it.remove();
		return new ArrayList<>(ENTRIES);
	}
}
