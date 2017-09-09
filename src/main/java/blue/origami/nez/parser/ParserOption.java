package blue.origami.nez.parser;

import blue.origami.util.OOption.OOptionKey;

public enum ParserOption implements OOptionKey {
	Verbose, WindowSize, Pass, Optimized, PassPath, StrictChecker, TrapActions, //
	TreeConstruction, PackratParsing, Coverage, GrammarFile, GrammarPath, Start, //
	PartialFailure, ThrowingParserError, InlineGrammar, InputFiles, Target, Debug;

	@Override
	public String toString() {
		return this.name();
	}

	@Override
	public OOptionKey keyOf(String key) {
		return ParserOption.valueOf(key);
	}
}
