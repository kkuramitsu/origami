package origami.main;

import origami.main.OOption.Key;

public enum ParserOption implements Key {
	WindowSize, Pass, Unoptimized, PassPath, StrictChecker, TrapActions, //
	TreeConstruction, PackratParsing, Coverage, GrammarFile, GrammarPath, Start, PartialFailure, ThrowingParserError, InlineGrammar, InputFiles;

	public String toString() {
		return name();
	}

	@Override
	public Key keyOf(String key) {
		return ParserOption.valueOf(key);
	}
}
