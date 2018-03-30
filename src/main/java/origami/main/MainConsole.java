package origami.main;

import java.util.Objects;

public interface MainConsole {

	// static boolean isColored = (System.getenv("CLICOLOR") != null);
	static boolean isColored = (System.getenv("TERM") != null);

	public final static String Bold = "\u001b[1m";
	public final static String Clear = "\u001b[00m";
	public final static String Red = "\u001b[00;31m";
	public final static String Green = "\u001b[00;32m";
	public final static String Yellow = "\u001b[00;33m";
	public final static String Blue = "\u001b[00;34m";
	public final static String Magenta = "\u001b[00;35m";
	public final static String Cyan = "\u001b[00;36m";
	public final static String Gray = "\u001b[00;37m";

	public default String c(String c, String text) {
		if (isColored) {
			return c + text + Clear;
		} else {
			return text;
		}
	}

	public default void c(String s, Runnable r) {
		if (isColored) {
			System.out.print(s);
			r.run();
			System.out.print(Clear);
		} else {
			r.run();
		}
	}

	public default void dump(String tab, Object o) {
		System.out.print(tab);
		String s = Objects.toString(o);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\n') {
				System.out.println();
				System.out.print(tab);
			} else {
				System.out.print(c);
			}
		}
		System.out.println();
	}

}
