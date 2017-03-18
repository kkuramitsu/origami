package origami.main;

import origami.rule.LocaleFormat;

public enum MainFmt implements LocaleFormat {
	error, warning, notice, //
	English_Edition, //
	Tips__starting_with_an_empty_line_for_multiple_lines;

	
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
