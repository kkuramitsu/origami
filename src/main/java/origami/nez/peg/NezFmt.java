package origami.nez.peg;

import origami.nez.ast.LocaleFormat;

public enum NezFmt implements LocaleFormat {
	error, warning, notice,//
	syntax_error, unconsumed, //
	YY0_is_duplicated_name;

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

	public String toString() {
		return stringfy(name());
	}
}
