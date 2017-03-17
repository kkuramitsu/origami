/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 *  *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package origami;

import origami.trait.OStringBuilder;

public class OConsole {

	public static void exit(int status, String message) {
		OConsole.println("EXIT " + message);
		System.exit(status);
	}

	//static boolean isColored = (System.getenv("CLICOLOR") != null);
	static boolean isColored = (System.getenv("TERM") != null);

	// 31 :red 32 green, 34 blue, 37 gray
	public final static int Red = 31;
	public final static int Green = 32;
	public final static int Yellow = 33;
	public final static int Blue = 34;
	public final static int Magenta = 35;
	public final static int Cyan = 36;
	public final static int Gray = 37;

	public static String bold(String text) {
		if (isColored) {
			return "\u001b[1m" + text + "\u001b[00m";
		}
		return text;
	}

	public static String color(int c, String text) {
		if (isColored) {
			return "\u001b[00;" + c + "m" + text + "\u001b[00m";
		}
		return text;
	}

	public static String bold(int c, String text) {
		if (isColored) {
			return "\u001b[00;" + c + "m" + "\u001b[1m" + text + "\u001b[00m";
		}
		return text;
	}

	public static void beginColor(int c) {
		if (isColored) {
			System.out.print("\u001b[00;" + c + "m");
		}
	}

	public static void endColor() {
		if (isColored) {
			System.out.print("\u001b[00m");
		}
	}

	public static void beginColor(StringBuilder sb, int c) {
		if (isColored) {
			sb.append("\u001b[00;" + c + "m");
		}
	}

	public static void endColor(StringBuilder sb) {
		if (isColored) {
			sb.append("\u001b[00m");
		}
	}

	public static void println(Object s) {
		System.out.println(s);
	}

	public static void print(Object s) {
		System.out.print(s);
	}

	public static void println(String format, Object... args) {
		System.out.println(OStringBuilder.format(format, args));
	}

	public static void print(String format, Object... args) {
		System.out.print(OStringBuilder.format(format, args));
	}

	public static void dump(String tab, Object o) {
		System.out.print(tab);
		String s = o.toString();
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
