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

		// Eliminate Left-Recursion
		LeftRecursionEliminator eliminator = options.newInstance(LeftRecursionEliminator.class);
		eliminator.compute(g);

		// result
		g.dump();
	}
}
