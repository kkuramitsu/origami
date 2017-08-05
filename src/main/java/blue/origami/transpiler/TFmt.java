package blue.origami.transpiler;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.util.OConsole;

public enum TFmt implements LocaleFormat {
	error, warning, notice, info, //
	// syntax analysis
	syntax_error, //
	version, undefined_syntax__YY0, undefined_name__YY0, wrong_number_format_YY0_by_YY1, //
	unsupported_operator, mixed_array_YY0_YY1, undefined_field_YY0_in_YY1, immutable_data, no_typing_hint__YY0, undefined_type__YY0, failed_type_inference, undefined_name__YY0__YY1, no_more_assignment; //

	public static String quote(Object o) {
		return OConsole.bold("'" + o + "'");
	}

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
