package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.util.OLog;

public interface TCodeSection {
	public void push(String t);

	public void push(TCode t);

	public void pushLog(OLog log);
}
