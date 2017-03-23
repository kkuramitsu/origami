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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import origami.OVersion;
import origami.nez.ast.LocaleFormat;
import origami.nez.ast.Tree;
import origami.nez.parser.Parser;
import origami.nez.peg.Grammar;
import origami.util.OConsole;
import origami.util.ODebug;
import origami.util.OOption;
import origami.util.StringCombinator;
import origami.util.OOption.Key;

public abstract class OCommand extends OConsole {

	public static void main(String[] args) {
		OOption options = new OOption();
		try {
			OCommand com = newCommand(args, options);
			com.exec(options);
		} catch (Throwable e) {
			e.printStackTrace();
			exit(1, e);
		}
	}

	public static void start(String... args) throws Throwable {
		OOption options = new OOption();
		OCommand com = newCommand(args, options);
		com.exec(options);
	}

	private static OCommand newCommand(String[] args, OOption options) {
		try {
			String className = args.length == 0 ? "hack" : args[0];
			if (className.indexOf('.') == -1) {
				className = "origami.main.O" + className;
			}
			OCommand cmd = (OCommand) Class.forName(className).newInstance();
			cmd.initOption(options);
			cmd.parseCommandOption(args, options);
			return cmd;
		} catch (Exception e) {
			usage("unknown command by " + e);
			return null;
		}
	}

	public abstract void exec(OOption options) throws Throwable;

	protected void initOption(OOption options) {
		options.set(ParserOption.GrammarPath, new String[] { "/origami/grammar", "/nez/lib" });
	}

	static HashMap<String, Key> optMap = new HashMap<>();
	static {
		optMap.put("-g", ParserOption.GrammarFile);
		optMap.put("--grammar", ParserOption.GrammarFile);
		optMap.put("-p", ParserOption.GrammarFile);
		optMap.put("-e", ParserOption.InlineGrammar);
		optMap.put("--expression", ParserOption.InlineGrammar);
		optMap.put("-s", ParserOption.Start);
		optMap.put("--start", ParserOption.Start);
		// optMap.put("-f", "format");
		// optMap.put("--format", "format");
		// optMap.put("-d", "dir");
		// optMap.put("--dir", "dir");
	}

	private void parseCommandOption(String[] args, OOption options) {
		ArrayList<String> fileList = new ArrayList<>();
		for (int index = 1; index < args.length; index++) {
			String as = args[index];
			Key key = optMap.get(as);
			if (key != null && index + 1 < args.length) {
				options.set(key, args[index + 1]);
				index++;
				continue;
			}
			if (as.startsWith("-D")) {
				options.setKeyValue(as.substring(2), ParserOption.Start);
				continue;
			}
			if (as.startsWith("-X")) {
				try {
					options.setClass(as.substring(2));
				} catch (Throwable e) {
					OConsole.println("unfound class " + as);
				}
				continue;
			}
			if (!as.startsWith("-")) {
				fileList.add(as);
				continue;
			}
			usage("undefined option: " + as);
		}
		options.set(ParserOption.InputFiles, fileList.toArray(new String[fileList.size()]));
	}

	protected Grammar getGrammar(OOption options, String file) throws IOException {
		file = options.value(ParserOption.GrammarFile, file);
		if (file == null) {
			exit(1, MainFmt.no_specified_grammar);
		}
		return Grammar.loadFile(file, options.list(ParserOption.GrammarPath));
	}

	protected Grammar getGrammar(OOption options) throws IOException {
		return this.getGrammar(options, null);
	}

	protected Parser getParser(OOption options) throws IOException {
		Grammar g = this.getGrammar(options);
		return g.newParser(options);
	}

	protected static void displayVersion(String codeName) {
		p(bold(OVersion.ProgName) + "-" + OVersion.Version + " (" + codeName + "," + MainFmt.English + ") on Java JVM-"
				+ System.getProperty("java.version"));
		p(Yellow, OVersion.Copyright);
	}

	protected static void usage(String msg) {
		displayVersion("Celery");
		p(bold("Usage: origami <command> options inputs"));
		p2("  -g | --grammar <file>      ", MainFmt.specify_a_grammar_file);
		p2("  -s | --start <NAME>        ", MainFmt.specify_a_starting_rule);
		p2("  -X                         ", MainFmt.specify_an_extension_class);
		p2("  -D                         ", MainFmt.specify_an_optional_value);
		p("Example:");
		p("  origami run sample.iroha");
		p("  origami example -g js.nez");
		p("  origami parse -g js.nez -X JsonWriter jquery.js");
		p("");

		p(bold("The most commonly used origami commands are:"));
		p2("  run      ", MainFmt.run_script_files);
		p2("  hack     ", MainFmt.run_in_a_hacker_mode);
		p2("  check    ", MainFmt.test_script_files);
		p2("  parse    ", MainFmt.parse_files);
		p2("  example  ", MainFmt.display_examples_in_a_grammar);
		p2("  test     ", MainFmt.test_a_grammar_file);
		p2("  nez      ", MainFmt.run_an_interactive_parser);
		exit(0, msg);
	}

	public final void checkInputSource(String[] files) {
		if (files == null || files.length == 0) {
			exit(1, MainFmt.no_specified_inputs.toString());
		}
	}

	public final static void p(String fmt, Object... args) {
		println(StringCombinator.format(fmt, args));
	}

	static void p2(String desc, LocaleFormat fmt, Object... args) {
		print(desc);
		println(StringCombinator.format(fmt, args));
	}

	public final static void p(int color, String fmt, Object... args) {
		beginColor(color);
		println(StringCombinator.format(fmt, args));
		endColor();
	}

	public final static void p(int color, LocaleFormat fmt, Object... args) {
		beginColor(color);
		println(StringCombinator.format(fmt, args));
		endColor();
	}

	public final static void display(Grammar g) {
		beginColor(Blue);
		g.dump();
		endColor();
	}

	public final static void display(OTreeWriter w, Tree<?> t) {
		beginColor(Blue);
		println("-----------");
		w.writeln(t);
		endColor();
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
		if (this.console == null) {
			try {
				this.console = Class.forName("jline.ConsoleReader").newInstance();
			} catch (Exception e) {
			}
		}
		if (this.console == null) {
			try {
				this.console = Class.forName("jline.console.ConsoleReader").newInstance();
			} catch (Exception e) {
			}
		}
		if (this.console == null && !this.consoleNotFound) {
			this.consoleNotFound = true;
			ODebug.FIXME("Jline is not found!!");
		}
		return this.console;
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
		String line = this.readSingleLine(prompt);
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
			line = this.readSingleLine("");
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
		this.linenum += linecount;
		return sb.toString();
	}

}
