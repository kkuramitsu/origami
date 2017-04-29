package blue.nez.parser;

import blue.origami.util.OptionalFactory;

public interface ParserCompiler extends OptionalFactory<ParserCompiler> {
	public ParserExecutable compile(ParserGrammar grammar);

	@Override
	public default Class<?> keyClass() {
		return ParserCompiler.class;
	}
}