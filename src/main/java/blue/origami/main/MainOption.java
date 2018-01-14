package blue.origami.main;

import blue.origami.common.OOption.OOptionKey;

public enum MainOption implements OOptionKey {
	Verbose, WindowSize, Pass, Optimized, PassPath, StrictChecker, TrapActions, //
	TreeConstruction, PackratParsing, Coverage, GrammarFile, GrammarPath, Start, //
	PartialFailure, ThrowingParserError, InlineGrammar, InputFiles, Target, Language, Debug;

	@Override
	public String toString() {
		return this.name();
	}

	@Override
	public OOptionKey keyOf(String key) {
		return MainOption.valueOf(key);
	}
}
