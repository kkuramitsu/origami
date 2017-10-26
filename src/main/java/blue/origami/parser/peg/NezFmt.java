package blue.origami.parser.peg;

import blue.origami.common.OFormat;

public enum NezFmt implements OFormat {
	error, warning, notice, //
	syntax_error, unconsumed, //
	YY1_is_duplicated_name, //
	YY1_is_undefined_grammar, //
	YY1_is_undefined_terminal, //
	YY1_is_undefined_nonterminal, //
	left_recursion_is_forbidden__YY1, //
	removed_YY1;

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
