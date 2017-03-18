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

import java.lang.reflect.Method;

import origami.nez.ast.SourcePosition;
import origami.rule.OFmt;
import origami.trait.StringCombinator;
import origami.trait.OTypeUtils;

public class ODebug extends OConsole {

	public static boolean enabled = false;

	public static void setDebug(boolean b) {
		enabled = b;
	}

	public final static boolean isDebug() {
		return enabled || System.getenv("DEBUG") != null;
	}

	public static String loc(StackTraceElement s) {
		return color(Gray, "@[" + s.getClassName() + "." + s.getMethodName() + "] ");
	}

	public static String filenum(StackTraceElement s) {
		return color(Gray, "@[" + s.getFileName() + ":" + s.getLineNumber() + "] ");
	}

	public static void trace(String fmt, Object... args) {
		if (isDebug()) {
			StackTraceElement[] s = Thread.currentThread().getStackTrace();
			println(loc(s[2]) + StringCombinator.format(fmt, args));
		}
	}

	public static void trace2(String fmt, Object... args) {
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		println(loc(s[2]) + StringCombinator.format(fmt, args));
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
		println("[TODO] " + StringCombinator.format(fmt, args));
	}

	public static void FIXME(String s) {
		println("[FIXME] " + s);
	}

	// -- assert --

	private static int testCount = 0;
	private static int passCount = 0;

	public static final Method AssertMethod = OTypeUtils.loadMethod(ODebug.class, "assertTest", boolean.class, String.class);

	public static String assertMessage(OEnv env, SourcePosition s) {
		return SourcePosition.formatErrorMessage(s, OFmt.assertion_failed);
	}

	public final static void assertTest(boolean b, String msg) {
		testCount++;
		assert (b) : msg;
		passCount++;
	}

	public final static int getTestCount() {
		return testCount;
	}

	public final static int getPassCount() {
		return passCount;
	}

	public final static void resetCount() {
		testCount = 0;
		passCount = 0;
	}

}
