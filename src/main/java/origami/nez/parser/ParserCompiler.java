package origami.nez.parser;

import origami.main.OOption;
import origami.nez.peg.Grammar;

public interface ParserCompiler extends OOption.OptionalFactory<ParserCompiler> {
	public ParserExecutable compile(Grammar grammar);
	public default Class<?> entryClass() {
		return ParserCompiler.class;
	}
}