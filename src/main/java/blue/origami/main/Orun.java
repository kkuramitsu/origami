
package blue.origami.main;

import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.Grammar;
import blue.origami.transpiler.Transpiler;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public class Orun extends OCommand {
	@Override
	public void exec(OOption options) throws Throwable {
		String[] files = options.stringList(ParserOption.InputFiles);
		String target = options.stringValue(ParserOption.Target, "jvm");
		Grammar g = this.getGrammar(options, "konoha5.opeg");
		Transpiler env = new Transpiler(g, target, options);
		// ODebug.setVerbose(this.isDebug());
		ODebug.setDebug(this.isDebug());
		for (String file : files) {
			env.loadScriptFile(file);
		}
		if (files.length == 0 || this.isDebug()) {
			displayVersion("Konoha5->" + target);
			p(Yellow, MainFmt.Tips__starting_with_an_empty_line_for_multiple_lines);
			// p("");

			int startline = this.linenum;
			String prompt = bold("\n>>> ");
			String input = null;
			while ((input = this.readMulti(prompt)) != null) {
				if (checkEmptyInput(input)) {
					continue;
				}
				env.eval("<stdin>", startline, input);
				startline = this.linenum;
			}
		}
	}

	public boolean isDebug() {
		return false;
	}

}
