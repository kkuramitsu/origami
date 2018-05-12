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

import blue.origami.PatchLevel;
import blue.origami.common.OConsole;
import blue.origami.common.ODebug;
import blue.origami.common.OFormat;
import blue.origami.common.OOption;
import blue.origami.common.OOption.OOptionKey;
import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.SourceGrammar;

public abstract class Main implements MainConsole {

	public final static String ProgName = "ORIGAMI";
	public final static String CodeName = "Celery";

	public final static int MainVersion = 0;
	public final static int MinorVersion = 3;
	public final static String Version = "" + MainVersion + "." + MinorVersion + "." + PatchLevel.REV;
	public final static String Copyright = "Copyright 2018, Kimio Kuramitsu and ORIGAMI project";
	public final static String License = "the Apache License, Version 2.0";

	public final static String ClassPath = "origami";
	public final static String ResourcePath = "/origami";

	public static void main(String[] args) {
		OOption options = new OOption();
		try {
			Main com = loadCommand(args, options);
			com.exec(options);
		} catch (Throwable e) {
			e.printStackTrace();
			exit(1, e);
		}
	}

	public static void testMain(String... args) throws Throwable {
		OOption options = new OOption();
		Main com = loadCommand(args, options);
		com.exec(options);
	}

	private static Main loadCommand(String[] args, OOption options) {
		try {
			String className = args.length == 0 ? "parse" : args[0];
			if (className.indexOf('.') == -1) {
				className = ClassPath + ".main.O" + className;
			}
			Main cmd = (Main) Class.forName(className).newInstance();
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
		options.set(MainOption.GrammarPath, new String[] { ResourcePath + "/grammar", "/blue/local" });
	}

	static HashMap<String, OOptionKey> optMap = new HashMap<>();

	static {
		optMap.put("-g", MainOption.GrammarFile);
		optMap.put("--grammar", MainOption.GrammarFile);
		optMap.put("-p", MainOption.GrammarFile);
		optMap.put("-e", MainOption.InlineGrammar);
		optMap.put("--expression", MainOption.InlineGrammar);
		optMap.put("-s", MainOption.Start);
		optMap.put("--start", MainOption.Start);
		optMap.put("-f", MainOption.FromFile);

	}

	private void parseCommandOption(String[] args, OOption options) {
		ArrayList<String> fileList = new ArrayList<>();
		for (int index = 1; index < args.length; index++) {
			String as = args[index];
			OOptionKey key = optMap.get(as);
			if (key != null && index + 1 < args.length) {
				options.set(key, args[index + 1]);
				index++;
				continue;
			}
			if (as.startsWith("-D")) {
				options.setKeyValue(as.substring(2), MainOption.Start);
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
			if (as.startsWith("--verbose")) {
				options.setVerbose(true);
				continue;
			}
			if (!as.startsWith("-")) {
				fileList.add(as);
				continue;
			}
			usage("undefined option: " + as);
		}
		options.set(MainOption.InputFiles, fileList.toArray(new String[fileList.size()]));
	}

	protected Grammar getGrammar(OOption options, String file) throws IOException {
		file = options.stringValue(MainOption.GrammarFile, file);
		if (file == null) {
			exit(1, MainFmt.no_specified_grammar);
		}
		return SourceGrammar.loadFile(file, options.stringList(MainOption.GrammarPath));
	}

	// protected Grammar getGrammar(OOption options) throws IOException {
	// return this.getGrammar(options, null);
	// }
	//
	// protected Parser getParser(OOption options) throws IOException {
	// Grammar g = this.getGrammar(options);
	// return g.newParser(options);
	// }

	void displayVersion() {
		this.c(Bold, () -> {
			p(this.progName() + "-" + this.version() + " (" + MainFmt.English + ") on Java JVM-"
					+ System.getProperty("java.version") + "/" + ProgName + "-" + Version);
		});
		this.c(Yellow, () -> {
			p(Copyright);
		});
	}

	String progName() {
		return "Origami";
	}

	String version() {
		return Version;
	}

	static void p(String msg, Object... args) {
		if (args.length == 0) {
			System.out.println(msg);
		} else {
			System.out.printf(msg, args);
			System.out.println();
		}
	}

	static void usage(String msg) {
		// this.displayVersion();
		p("Usage: origami <command> options inputs");
		p("  -g | --grammar <file>      " + MainFmt.specify_a_grammar_file);
		p("  -s | --start <NAME>        " + MainFmt.specify_a_starting_rule);
		p("  -X                         " + MainFmt.specify_an_extension_class);
		p("  -D                         " + MainFmt.specify_an_optional_value);
		p("Example:");
		p("  origami nez -g js.nez -X JsonWriter jquery.js");
		p("  origami parse -g js.nez -X JsonWriter jquery.js");
		p("  origami chibi sample.chibi");
		p("");

		p("The most commonly used origami commands are:");
		p(" nez        " + MainFmt.run_an_interactive_parser);
		p(" nezcc      " + MainFmt.generate_nez_parser);
		p("   parse    " + MainFmt.parse_files);
		p("   example  " + MainFmt.display_examples_in_a_grammar);
		p("   test     " + MainFmt.test_a_grammar_file);
		p(" chibi      " + MainFmt.run_script_files);
		p("   hack     " + MainFmt.run_in_a_hacker_mode);
		p("   check    " + MainFmt.test_script_files);
		exit(0, msg);
	}

	public final void checkInputSource(String[] files) {
		if (files == null || files.length == 0) {
			exit(1, MainFmt.no_specified_inputs.toString());
		}
	}

	// Console
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
	// private ConsoleReader console = null;
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
				((jline.console.ConsoleReader) this.console).setExpandEvents(false);
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

	// OConsole

	public static void exit(int status, Throwable e) {
		System.out.println("EXIT by " + e);
		e.printStackTrace();
		System.exit(status);
	}

	public static void exit(int status, String msg) {
		System.out.println("EXIT by " + msg);
		System.exit(status);
	}

	public static void exit(int status, OFormat message) {
		OConsole.println("EXIT " + message);
		System.exit(status);
	}

}
