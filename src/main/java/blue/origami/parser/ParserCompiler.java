package blue.origami.parser;

import blue.origami.util.OFactory;

public interface ParserCompiler extends OFactory<ParserCompiler> {
	public ParserCode compile(ParserGrammar grammar);

	@Override
	public default Class<?> keyClass() {
		return ParserCompiler.class;
	}
}