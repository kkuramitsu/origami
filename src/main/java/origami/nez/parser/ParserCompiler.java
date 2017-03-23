package origami.nez.parser;

import origami.nez.peg.Grammar;
import origami.util.OOption;

public interface ParserCompiler extends OOption.OptionalFactory<ParserCompiler> {
	public ParserExecutable compile(Grammar grammar);

	public default Class<?> entryClass() {
		return ParserCompiler.class;
	}
}