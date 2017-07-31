package blue.origami.main;

import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.LeftRecursionEliminator;
import blue.origami.nez.peg.SourceGrammar;
import blue.origami.util.OOption;

public class Okourin extends OCommand {

	@Override
	public void exec(OOption options) throws Throwable {
		String[] files = options.stringList(ParserOption.InputFiles);
		Grammar g = SourceGrammar.loadFile(files[0]);
		System.out.println("=== original grammar ===");
		g.dump();
		System.out.println();

		LeftRecursionEliminator eliminator = options.newInstance(LeftRecursionEliminator.class);
		eliminator.compute(g);
		System.out.println("=== converted grammar ===");
		g.dump();
		System.out.println();

		/*
		 * Parser parser = new Parser(g.getStartProduction(), options);
		 * parser.compile(); System.out.println("=== compiled grammar ===");
		 * parser.getParserGrammar().dump(); System.out.println();
		 */
	}
}
