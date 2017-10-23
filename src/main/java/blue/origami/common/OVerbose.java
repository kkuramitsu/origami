package blue.origami.common;

public class OVerbose {
	public static boolean enabled = true;

	public final static void println(String s) {
		if (enabled) {
			OConsole.beginColor(34);
			OConsole.println(s);
			OConsole.endColor();
		}
	}

	public final static void println(String fmt, Object... args) {
		if (enabled) {
			println(String.format(fmt, args));
		}
	}

	public final static void print(String s) {
		if (enabled) {
			OConsole.beginColor(34);
			OConsole.print(s);
			OConsole.endColor();
		}
	}

	public final static void print(String fmt, Object... args) {
		if (enabled) {
			print(String.format(fmt, args));
		}
	}

	public final static void printElapsedTime(String msg, long t1, long t2) {
		if (enabled) {
			double d = (t2 - t1) / 1000000;
			if (d > 0.1) {
				println("%s : %f[ms]", msg, d);
			}
		}
	}

}
