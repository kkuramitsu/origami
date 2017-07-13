package blue.origami.main;

import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.Grammar;
import blue.origami.transpiler.Transpiler;
import blue.origami.util.OOption;

public class Okonoha extends OCommand {

	@Override
	public void exec(OOption options) throws Throwable {
		String[] files = options.stringList(ParserOption.InputFiles);
		if (options.stringValue(ParserOption.GrammarFile, null) == null) {
			if (files.length > 0) {
				String ext = SourcePosition.extractFileExtension(files[0]);
				options.set(ParserOption.GrammarFile, ext + ".opeg");
			}
		}
		Grammar g = this.getGrammar(options, "iroha.opeg");
		Transpiler env = new Transpiler(g, "java8", options);

		for (String file : files) {
			env.loadScriptFile(file);
		}
		if (files.length == 0 || this.isDebug()) {
			displayVersion(g.getName());
			p(Yellow, MainFmt.Tips__starting_with_an_empty_line_for_multiple_lines);
			p("");

			int startline = this.linenum;
			String prompt = bold(">>> ");
			String input = null;
			while ((input = this.readMulti(prompt)) != null) {
				if (checkEmptyInput(input)) {
					continue;
				}
				env.shell("<stdin>", startline, input);
				startline = this.linenum;
			}
		}
	}

	public boolean isDebug() {
		return false;
	}

}
