package blue.origami.transpiler;

import blue.origami.common.OConsole;
import blue.origami.common.OFormat;

public enum TFmt implements OFormat {
	error, warning, notice, info, version, //
	// Hack mode
	ParserError, //
	Syntax_Tree, Typed_Code, Template, Template_Specialization, Generated_ByteCode,
	// Message
	Checked,
	// syntax analysis
	syntax_error, unsupported_error, //
	undefined_syntax__YY1, undefined_name__YY1, undefined_name__YY1__YY2, //
	already_defined_YY1_as_YY2, //
	wrong_number_format_YY1_by_YY2, //
	no_type_hint__YY1, undefined_type__YY1, failed_type_inference, undefined_name__YY1_in_YY2, //
	no_more_assignment, not_function__YY1, hint, YY1_does_not_exist_in_YY2, YY1_have_a_YY2_type, //
	redefined_name__YY1, type_error_YY1_YY2, patterns_are_different, required_first_argument, //
	undefined_SSS, mismatched_SSS, mismatched_parameter_size_S_S, not_mutable_SSS, //
	function_S_remains_undefined, ambiguous_type__S, abstract_function_YY1__YY2, //
	not_tuple, bad_tuple__YY1, not_name, //
	// imperative languages
	immutable_name__YY1, mutable_cast_to_YY1, mutable_name__YY1, //
	YY1_cannot_be_used, YY1_cannot_be_used_in_YY2, //
	let_mut, while_loop, break_statement, return_statement, assign, switch_statement; //

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
