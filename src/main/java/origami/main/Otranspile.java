package origami.main;

import java.io.IOException;
import java.util.Arrays;

import blue.origami.PatchLevel;
import blue.origami.common.ODebug;
import blue.origami.common.OOption;
import blue.origami.transpiler.Transpiler;

public class Otranspile extends Main {

	@Override
	public void exec(OOption options) throws Throwable {
		String[] files = options.stringList(MainOption.InputFiles);
		Transpiler[] trcc = this.newTranspiler(options);
		for (String file : files) {
			this.loadScriptFile(trcc, file);
		}
		ODebug.setHacked(this.isHacked());
		ODebug.setVerbose(options.is(MainOption.Verbose, this.isVerbose()));
		ODebug.setDebug(options.is(MainOption.Debug, this.isDebug()));
		Arrays.stream(trcc).forEach(tr -> {
			tr.setShellMode(true);
		});
		if (files.length == 0 || this.isREPL()) {
			this.displayVersion();
			p(Yellow, MainFmt.Tips__starting_with_an_empty_line_for_multiple_lines);

			int startline = this.linenum;
			// String prompt = bold("\n>>> ");
			String prompt = this.c(Bold, "\n🍃>>> ");
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
		return "Chibi🍃";
	}

	@Override
	protected String version() {
		return "5.0." + PatchLevel.REV;
	}

	private Transpiler[] newTranspiler(OOption options) throws Throwable {
		return new Transpiler[] { options.newInstance(Transpiler.class) };
	}

	// public String getDefaultTarget() {
	// return "Jvm8";
	// }

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