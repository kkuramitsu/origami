package blue.nez.parser;

import blue.origami.util.OOption;

public interface ParserCompiler extends OOption.OptionalFactory<ParserCompiler> {
	public ParserExecutable compile(ParserGrammar grammar);

	@Override
	public default Class<?> entryClass() {
		return ParserCompiler.class;
	}
}