package blue.nez.parser;

import blue.nez.peg.Grammar;
import blue.origami.util.OOption;

public interface ParserCompiler extends OOption.OptionalFactory<ParserCompiler> {
	public ParserExecutable compile(Grammar grammar);

	public default Class<?> entryClass() {
		return ParserCompiler.class;
	}
}