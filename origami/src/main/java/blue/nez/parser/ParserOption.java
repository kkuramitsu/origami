package blue.nez.parser;

import blue.origami.util.OOption.OOptionKey;

public enum ParserOption implements OOptionKey {
	WindowSize, Pass, Unoptimized, PassPath, StrictChecker, TrapActions, //
	TreeConstruction, PackratParsing, Coverage, GrammarFile, GrammarPath, Start, //
	PartialFailure, ThrowingParserError, InlineGrammar, InputFiles;

	@Override
	public String toString() {
		return this.name();
	}

	@Override
	public OOptionKey keyOf(String key) {
		return ParserOption.valueOf(key);
	}
}
