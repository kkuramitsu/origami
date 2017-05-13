package blue.nez.parser;

import blue.origami.util.OptionalFactory;

public interface ParserCompiler extends OptionalFactory<ParserCompiler> {
	public ParserCode compile(ParserGrammar grammar);

	@Override
	public default Class<?> keyClass() {
		return ParserCompiler.class;
	}
}