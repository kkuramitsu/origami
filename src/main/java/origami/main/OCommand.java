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

package origami.main;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Scanner;

import origami.OConsole;
import origami.ODebug;
import origami.OVersion;
import origami.main.tool.CommonWriter;
import origami.nez.ast.Tree;
import origami.nez.parser.ParserFactory;
import origami.nez.parser.ParserFactory.GrammarWriter;
import origami.nez.parser.ParserFactory.TreeWriter;
import origami.nez.peg.OGrammar;
import origami.trait.OStringBuilder;

public abstract class OCommand extends OConsole {

	public final static String ProgName = "ORIGAMI";
	public final static String CodeName = "Celery";
	
	public final static int MajorVersion = 0;
	public final static int MinerVersion = 0;
	public final static int PatchLevel = 1;
	public static void main(String[] args) {
		ParserFactory fac = new ParserFactory();
		fac.set("grammar-path", new String[] { "/origami/grammar", "/nez/lib" });
		try {
			OCommand com = newCommand(args, fac);
			fac.verbose("nez-%d.%d.%d %s", MajorVersion, MinerVersion, PatchLevel, com.getClass().getName());
			fac.verbose("fac: %s", fac);
			com.exec(fac);
		} catch (IOException e) {
			System.err.println(e);
			fac.trace(e);
			System.exit(1);
		}
	}

	private static OCommand newCommand(String[] args, ParserFactory fac) {
		try {
			String className = args[0];
			if (className.indexOf('.') == -1) {
				className = "origami.main.O" + className;
			}
			OCommand cmd = (OCommand) Class.forName(className).newInstance();
			cmd.parseCommandOption(args, fac);
			return cmd;
		} catch (Exception e) {
			usage("unknown command by " + e);
			return null;
		}
	}

	public abstract void exec(ParserFactory fac) throws IOException;

	static HashMap<String, String> optMap = new HashMap<>();
	static {
		optMap.put("-g", "grammar");
		optMap.put("--grammar", "grammar");
		optMap.put("-p", "grammar");
		optMap.put("-e", "expression");
		optMap.put("--expression", "expression");
		optMap.put("-s", "start");
		optMap.put("--start", "start");
		optMap.put("-f", "format");
		optMap.put("--format", "format");
		optMap.put("-d", "dir");
		optMap.put("--dir", "dir");
	}

	private void parseCommandOption(String[] args, ParserFactory fac) throws IOException {
		for (int index = 1; index < args.length; index++) {
			String as = args[index];
			String key = optMap.get(as);
			if (key != null && index + 1 < args.length) {
				fac.set(key, args[index + 1]);
				index++;
				continue;
			}
			if (as.startsWith("-D")) {
				fac.setOption(as.substring(2));
				continue;
			}
			if (as.startsWith("-X")) {
				fac.loadClass(as.substring(2));
				continue;
			}
			if (as.equals("--verbose")) {
				fac.setVerboseMode(true);
				continue;
			}
			if (!as.startsWith("-")) {
				fac.add("files", as);
				continue;
			}
			usage("undefined option: " + as);
		}
	}

	protected static void displayVersion() {
		p(bold(ProgName + "-" + OVersion.Version + " (" + CodeName + ") on Java JVM-" + System.getProperty("java.version")));
		p(Blue, OVersion.Copyright);
	}

	protected void displayVersion(String progName, String version) {
		p(bold(progName + "-" + version + " (" + System.getenv("USER") + ") on Nez-" + OVersion.Version));
	}

	protected static void usage(String msg) {
		displayVersion();
		p(bold("Usage: nez <command> options inputs"));
		p("  -g | --grammar <file>      Specify a grammar file");
		p("  -f | --format <string>     Specify an output format");
		// p(" -e <text> Specify a Nez parsing expression");
		// p(" -a <file> Specify a Nez auxiliary grammar
		// files");
		p("  -s | --start <NAME>        Specify a starting production");
		p("  -d | --dir <dirname>       Specify an output dir");
		p("Example:");
		p("  nez parse -g js.nez jquery.js --format json");
		p("  nez match -g js.nez *.js");
		p("  nez parser -g math.nez --format c");
		p("");

		p(bold("The most commonly used nez commands are:"));
		p("  parse      parse inputs and construct ASTs");
		p("  match      match inputs without ASTs");
		p("  inez       an interactive parser");
		p("  code       generate a parser source code for --format");
		p("  cnez       generate a C-based fast parser");
		p("  peg        translate a grammar into PEG specified with --format");
		p("  compile    compile a grammar into Nez bytecode .moz");
		p("  bench      perform benchmark tests");
		p("  example    display examples in a grammar");
		p("  test       perform grammar tests");
		exit(0, msg);
	}

	public final void checkInputSource(String[] files) {
		if (files == null || files.length == 0) {
			OConsole.exit(1, "no input specified");
		}
	}

	public final static void p(String fmt, Object... args) {
		OConsole.println(OStringBuilder.format(fmt, args));
	}

	public final static void p(int color, String fmt, Object... args) {
		OConsole.beginColor(color);
		OConsole.println(OStringBuilder.format(fmt, args));
		OConsole.endColor();
	}

	public final static void begin(int color) {
		OConsole.beginColor(color);
	}

	public final static void end() {
		OConsole.endColor();
	}

	public final static void display(ParserFactory fac, GrammarWriter w, OGrammar g) {
		OConsole.beginColor(Blue);
		if (w instanceof CommonWriter) {
			((CommonWriter) w).Begin("---");
		}
		w.writeGrammar(fac, g);
		if (w instanceof CommonWriter) {
			((CommonWriter) w).End("---");
			((CommonWriter) w).L();
		}
		OConsole.endColor();
	}

	public final static void display(ParserFactory fac, TreeWriter w, Tree<?> t) {
		OConsole.beginColor(Blue);
		if (w instanceof CommonWriter) {
			((CommonWriter) w).Begin("---");
		}
		w.writeTree(fac, t);
		if (w instanceof CommonWriter) {
			((CommonWriter) w).End("---");
			((CommonWriter) w).L();
		}
		OConsole.endColor();
	}

	public final static String bold(String text) {
		return OConsole.bold(text);
	}

	public final static void exit(int status, String format, Object... args) {
		p(Red, format, args);
		System.exit(status);
	}

	public final static boolean checkEmptyInput(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
				continue;
			}
			return false;
		}
		return true;
	}

	// ReadLine
	private Object console = null;
	private boolean consoleNotFound = false;
	protected int linenum = 1;

	private final Object getConsole() {
		if (console == null) {
			try {
				console = Class.forName("jline.ConsoleReader").newInstance();
			} catch (Exception e) {
			}
		}
		if (console == null) {
			try {
				console = Class.forName("jline.console.ConsoleReader").newInstance();
			} catch (Exception e) {
			}
		}
		if (console == null && !consoleNotFound) {
			this.consoleNotFound = true;
			ODebug.FIXME("Jline is not found!!");
		}
		return console;
	}

	@SuppressWarnings("resource")
	public final String readSingleLine(String prompt) {
		Object c = this.getConsole();
		if (c != null) {
			try {
				Method m = c.getClass().getMethod("readLine", String.class);
				return (String) m.invoke(c, prompt);
			} catch (Exception e) {
				ODebug.traceException(e);
			}
		}
		System.out.print(prompt);
		System.out.flush();
		return new Scanner(System.in).nextLine();
	}

	protected final void addHistory(String text) {
		Object c = this.getConsole();
		if (c != null) {
			try {
				Method m = c.getClass().getMethod("getHistory");
				Object hist = m.invoke(c);
				m = hist.getClass().getMethod("addToHistory", String.class);
				m.invoke(hist, text);
			} catch (Exception e) {
				ODebug.traceException(e);
			}
		}
	}

	protected final String readMulti(String prompt) {
		StringBuilder sb = new StringBuilder();
		String line = readSingleLine(prompt);
		if (line == null) {
			return null;
		}
		int linecount = 0;
		boolean hasNext = false;
		if (line.equals("")) {
			hasNext = true;
		} else if (line.endsWith("\\")) {
			hasNext = true;
			sb.append(line.substring(0, line.length() - 1));
			linecount = 1;
		} else {
			linecount = 1;
			sb.append(line);
		}
		while (hasNext) {
			line = readSingleLine("");
			if (line == null) { // cancel
				return null;
			}
			if (line.equals("")) {
				break;
			} else if (line.endsWith("\\")) {
				if (linecount > 0) {
					sb.append("\n");
				}
				sb.append(line.substring(0, line.length() - 1));
				linecount++;
			} else {
				if (linecount > 0) {
					sb.append("\n");
				}
				sb.append(line);
				linecount++;
			}
		}
		if (linecount > 1) {
			sb.append("\n");
		}
		linenum += linecount;
		return sb.toString();
	}
	
}
