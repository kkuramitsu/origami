package blue.origami.main;

import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.Production;
import blue.origami.nez.peg.SourceGrammar;
import blue.origami.util.OOption;

public class Okoulin extends OCommand {

	@Override
	public void exec(OOption options) throws Throwable {
		String[] files = options.stringList(ParserOption.InputFiles);
		Grammar g = SourceGrammar.loadFile(files[0]);
		g.dump();
		Production p = g.getProduction("E");
		System.out.println(p);
	}

}
