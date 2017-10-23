package blue.origami.main;

import blue.origami.common.OFormat;

public enum MainFmt implements OFormat {
	error, warning, notice, //
	English, //
	Tips__starting_with_an_empty_line_for_multiple_lines, //
	specify_a_grammar_file, specify_a_starting_rule, //
	specify_an_extension_class, //
	specify_an_optional_value, //
	run_script_files, run_in_a_hacker_mode, test_script_files, //
	parse_files, display_examples_in_a_grammar, //
	test_a_grammar_file, run_an_interactive_parser, //
	no_specified_inputs, no_specified_grammar,
	// nez command
	grammar_is_successfully_loaded; //

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
