package origami.nez.ast;

import origami.OConsole;
import origami.trait.OStringBuilder;

public interface SourceLogger {

	public void reportError(SourceObject s, String fmt, Object... args);
	public void reportWarning(SourceObject s, String fmt, Object... args);
	public void reportNotice(SourceObject s, String fmt, Object... args);
	public void reportInfo(SourceObject s, String fmt, Object... args);

	public static class SimpleSourceLogger implements SourceLogger {
		final static int Error = 31;
		final static int Warning = 35;
		final static int Notice = 36;
		final static int Info = 37;

		void report(int level, String msg) {
			OConsole.beginColor(level);
			OConsole.println(msg);
			OConsole.endColor();
		}

		private String message(SourceObject s, String type, String fmt, Object... args) {
			if (s != null) {
				return s.formatSourceMessage(type, OStringBuilder.format(fmt, args));
			}
			return "(" + type + ") " + OStringBuilder.format(fmt, args);
		}

		public final void reportError(SourceObject s, String fmt, Object... args) {
			report(Error, message(s, "error", fmt, args));
		}
	
		public final void reportWarning(SourceObject s, String fmt, Object... args) {
			report(Warning, message(s, "warning", fmt, args));
		}
	
		public final void reportNotice(SourceObject s, String fmt, Object... args) {
			report(Notice, message(s, "notice", fmt, args));
		}
	
		public final void reportInfo(SourceObject s, String fmt, Object... args) {
			report(Info, message(s, "info", fmt, args));
		}
	}

}
