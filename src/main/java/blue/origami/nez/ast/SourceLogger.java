package blue.origami.nez.ast;

import blue.origami.util.OConsole;

public interface SourceLogger {

	public void reportError(SourcePosition s, LocaleFormat fmt, Object... args);

	public void reportWarning(SourcePosition s, LocaleFormat fmt, Object... args);

	public void reportNotice(SourcePosition s, LocaleFormat fmt, Object... args);

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

		public final void reportError(SourcePosition s, LocaleFormat fmt, Object... args) {
			report(Error, SourcePosition.formatErrorMessage(s, fmt, args));
		}

		public final void reportWarning(SourcePosition s, LocaleFormat fmt, Object... args) {
			report(Warning, SourcePosition.formatWarningMessage(s, fmt, args));
		}

		public final void reportNotice(SourcePosition s, LocaleFormat fmt, Object... args) {
			report(Notice, SourcePosition.formatNoticeMessage(s, fmt, args));
		}

	}

}
