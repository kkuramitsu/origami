package blue.origami.main;

import java.io.IOException;
import java.util.Arrays;

import blue.origami.PatchLevel;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.Grammar;
import blue.origami.transpiler.Transpiler;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public class Otranscode extends OCommand {

	@Override
	public void exec(OOption options) throws Throwable {
		String[] files = options.stringList(ParserOption.InputFiles);
		String target = options.stringValue(ParserOption.Target, "jvm");
		Transpiler[] trcc = this.newTranspiler(target, options);
		for (String file : files) {
			this.loadScriptFile(trcc, file);
		}
		ODebug.setHacked(this.isHacked());
		ODebug.setVerbose(options.is(ParserOption.Verbose, this.isVerbose()));
		ODebug.setDebug(options.is(ParserOption.Debug, this.isDebug()));
		Arrays.stream(trcc).forEach(tr -> {
			tr.setShellMode(true);
		});
		if (files.length == 0 || this.isREPL()) {
			this.displayVersion();
			p(Yellow, MainFmt.Tips__starting_with_an_empty_line_for_multiple_lines);

			int startline = this.linenum;
			// String prompt = bold("\n>>> ");
			String prompt = bold("\n(🍃" + PatchLevel.REV + ")>>> ");
			String input = null;
			while ((input = this.readMulti(prompt)) != null) {
				if (checkEmptyInput(input)) {
					continue;
				}
				this.shell(trcc, "<stdin>", startline, input + " ");
				startline = this.linenum;
			}
		}
	}

	@Override
	protected String progName() {
		return "Konoha 🍃";
	}

	@Override
	protected String version() {
		return "5.0." + PatchLevel.REV;
	}

	private Transpiler[] newTranspiler(String target, OOption options) throws Throwable {
		Grammar g = this.getGrammar(options, "konoha5.opeg");
		Parser p = g.newParser(options);
		if (target.indexOf(":") > 0) {
			String[] t = target.split(":", -1);
			return Arrays.stream(t).map((ta) -> new Transpiler(g, p, ta, options)).toArray(Transpiler[]::new);
		}
		return new Transpiler[] { new Transpiler(g, p, target, options) };
	}

	private void shell(Transpiler[] trcc, String source, int line, String script) {
		if (trcc.length == 1) {
			trcc[0].eval(source, line, script);
		} else {
			for (Transpiler tr : trcc) {
				tr.eval(source, line, script);
			}
		}
	}

	private void loadScriptFile(Transpiler[] trcc, String file) throws IOException {
		if (trcc.length == 1) {
			trcc[0].loadScriptFile(file);
		} else {
			for (Transpiler tr : trcc) {
				tr.loadScriptFile(file);
			}
		}
	}

	public boolean isREPL() {
		return false;
	}

	public boolean isHacked() {
		return false;
	}

	public boolean isVerbose() {
		return false;
	}

	public boolean isDebug() {
		return false;
	}

}