package blue.origami.nez.peg;

import blue.origami.nez.ast.LocaleFormat;

public enum NezFmt implements LocaleFormat {
	error, warning, notice, //
	syntax_error, unconsumed, //
	YY0_is_duplicated_name, //
	YY0_is_undefined_grammar, //
	YY0_is_undefined_terminal, //
	YY0_is_undefined_nonterminal, //
	left_recursion_is_forbidden__YY0;

	@Override
	public String error() {
		return error.toString();
	}

	@Override
	public String warning() {
		return warning.toString();
	}

	@Override
	public String notice() {
		return notice.toString();
	}

	@Override
	public String toString() {
		return this.stringfy(this.name());
	}
}
