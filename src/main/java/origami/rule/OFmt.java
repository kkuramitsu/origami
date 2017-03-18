package origami.rule;

import java.util.Locale;
import java.util.ResourceBundle;

import origami.OConsole;

public enum OFmt implements LocaleFormat {
	welcome, error, warning, notice, info, //
	// syntax analysis
	syntax_error, //
	not_assignable,
	// name analysis
	undefined, mismatched, read_only, //
	unfound, stupid, //
	name, type, syntax, clazz, constructor, //
	implicit_conversion, //
	S_is_not_here, //
	implicit_mutation, S_is_duplicated, S_is_unknown_name, __,
	// newly
	studpid_cast, nullable, //
	defineded, origami, unnecessary_expression, stupid_expression, implicit_type, //
	undefined_unit, not_clonable, S_is_meaningless, S_is_not_constant_value, S_must_be_S, assertion_failed, //
	undefined_name__YY0;

	public static String fmt(String fmt, OFmt... m) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < m.length; i++) {
			if (i > 0) {
				sb.append(__.toString());
			}
			sb.append(m[i].toString());
		}
		if (fmt != null) {
			sb.append(" ");
			sb.append(fmt);
		}
		return sb.toString();
	}

	public static String fmt(OFmt... m) {
		return fmt(null, m);
	}
	
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
		return stringfy(name());
	}

}
