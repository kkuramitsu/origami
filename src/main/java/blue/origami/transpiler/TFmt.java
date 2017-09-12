package blue.origami.transpiler;

import blue.origami.nez.ast.LocaleFormat;
import blue.origami.util.OConsole;

public enum TFmt implements LocaleFormat {
	error, warning, notice, info, version, //
	// Hack mode
	Syntax_Tree, Generated_ByteCode,
	// syntax analysis
	syntax_error, unsupported_error, //
	undefined_syntax__YY1, undefined_name__YY1, wrong_number_format_YY1_by_YY2, //
	no_type_hint__YY1, undefined_type__YY1, failed_type_inference, undefined_name__YY1_in_YY2, //
	no_more_assignment, not_function__YY1, hint, YY1_does_not_exist, YY1_have_a_YY2_type, //
	redefined_name__YY1, type_error_YY1_YY2, different_pattern, required_first_argument, //
	undefined_SSS, mismatched_SSS, mismatched_parameter_size_S_S; //

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
