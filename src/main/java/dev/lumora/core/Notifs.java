package dev.lumora.core;

import java.util.ArrayList;
import java.util.List;

/** Toast queue. Rendered by the Notifications module. */
public final class Notifs {
	private Notifs() {
	}

	public static final class Entry {
		public String title, msg;
		public boolean good;
		public long t0;
	}

	public static final List<Entry> LIST = new ArrayList<>();
	/** Set by the Notifications module so we can skip queueing when it is off. */
	public static boolean active = true;

	public static void push(String title, String msg, boolean good) {
		if (!active) return;
		Entry e = new Entry();
		e.title = title;
		e.msg = msg;
		e.good = good;
		e.t0 = System.currentTimeMillis();
		LIST.add(e);
		while (LIST.size() > 5) LIST.remove(0);
	}
}
