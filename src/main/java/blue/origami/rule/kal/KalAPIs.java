/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 *
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

package blue.origami.rule.kal;

import blue.origami.ffi.OrigamiObject;
import blue.origami.lang.OEnv;
import blue.origami.rule.cop.LayerRules;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class KalAPIs implements OrigamiObject {

	public final static <T> T p(T o, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, o));
		return o;
	}

	public final static String p(String a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static boolean p(boolean a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static int p(int a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static double p(double a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static long p(long a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static float p(float a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static short p(short a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static byte p(byte a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	public final static char p(char a, String file, int linenum, String expr, String type) {
		System.err.println(tweet(file, linenum, expr, type, a));
		return a;
	}

	private static String tweet(String file, int linenum, String expr, String type, Object o) {
		StringBuilder sb = new StringBuilder();
		sb.append(OConsole.color(ODebug.Blue, "#["));
		sb.append(OConsole.color(ODebug.Cyan, file));
		sb.append(OConsole.color(ODebug.Blue, ":"));
		sb.append(OConsole.color(ODebug.Cyan, "" + linenum));
		sb.append(OConsole.color(ODebug.Blue, "] "));
		sb.append(expr);
		sb.append(OConsole.color(ODebug.Red, ":"));
		sb.append(type);
		sb.append(OConsole.color(ODebug.Gray, " => "));
		StringCombinator.appendQuoted(sb, o);
		return sb.toString();
	}

	public final static void changeContext(OEnv env, String group, String context) {
		LayerRules.changeContext(env, group, context);
	}

}
