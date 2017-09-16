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

package blue.origami.util;

public class ODebug extends OConsole {

	private static boolean hacked = false;
	private static boolean traced = false;
	private static boolean debugged = false;

	public static void setHacked(boolean b) {
		hacked = b;
	}

	public static void setVerbose(boolean b) {
		traced = b;
	}

	public static void setDebug(boolean b) {
		debugged = b;
	}

	public static void showRed(String key, Runnable r) {
		show(key, Red, r);
	}

	public static void showMagenta(String key, Runnable r) {
		show(key, Magenta, r);
	}

	public static void showBlue(String key, Runnable r) {
		show(key, Blue, r);
	}

	public static void showCyan(String key, Runnable r) {
		show(key, Cyan, r);
	}

	private static void show(String key, int color, Runnable r) {
		if (hacked) {
			println(bold("[" + key + "]"));
			beginColor(color);
			r.run();
			endColor();
		}
	}

	public static void log(Runnable r) {
		if (traced) {
			r.run();
		}
	}

	public static void p(String fmt, Object... args) {
		// StackTraceElement[] s = Thread.currentThread().getStackTrace();
		println(/* loc(s[2]) + */ OStrings.format(fmt, args));
	}

	public static void stackTrace(String fmt, Object... args) {
		if (debugged) {
			StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
			println(loc(stacks[2]) + OStrings.format(fmt, args));
			int c = 0;
			for (StackTraceElement s : stacks) {
				if (c > 3 && c < 14) {
					println(String.format("[%d] %s", c, s));
				}
				c++;
			}
		}
	}

	public static String loc(StackTraceElement s) {
		return color(Gray, "" + s + " ");
	}

	public static void debug(Runnable r) {
		if (debugged) {
			r.run();
		}
	}

	public final static boolean isDebug() {
		return debugged || System.getenv("DEBUG") != null;
	}

	public static String filenum(StackTraceElement s) {
		return color(Gray, "@[" + s.getFileName() + ":" + s.getLineNumber() + "] ");
	}

	public static void trace(String fmt, Object... args) {
		if (isDebug()) {
			StackTraceElement[] s = Thread.currentThread().getStackTrace();
			println(OStrings.format(fmt, args) + " @ " + loc(s[2]));
		}
	}

	private static int countExceptions = 0;

	public static int countThrownException() {
		return countExceptions;
	}

	public static void traceException(Throwable e) {
		countExceptions++;
		if (isDebug()) {
			StackTraceElement[] s = e.getStackTrace();
			beginColor(Magenta);
			println("[Catching] " + loc(s[1]));
			e.printStackTrace(System.err);
			endColor();
		}
	}

	public static void NotAvailable() {
		if (isDebug()) {
			StackTraceElement[] s = Thread.currentThread().getStackTrace();
			println("[N/A] @" + loc(s[2]));
		}
	}

	public static void NotAvailable(Object callee) {
		if (isDebug()) {
			StackTraceElement[] s = Thread.currentThread().getStackTrace();
			println("[N/A] @" + loc(s[2]));
		}
	}

	public static void TODO(Object callee) {
		if (isDebug()) {
			StackTraceElement[] s = Thread.currentThread().getStackTrace();
			println("[TODO] @" + loc(s[2]));
		}
	}

	public static void FIXME(Object callee) {
		if (isDebug()) {
			StackTraceElement[] s = Thread.currentThread().getStackTrace();
			println("[FIXME] @" + loc(s[2]));
		}
	}

	public static void TODO() {
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		println("[TODO] " + s[2].getClassName() + "." + s[2].getMethodName());
	}

	public static void TODO(String s) {
		println("[TODO] " + s);
	}

	public static void TODO(String fmt, Object... args) {
		println("[TODO] " + OStrings.format(fmt, args));
	}

	public static void FIXME(String s) {
		println("[FIXME] " + s);
	}

}
