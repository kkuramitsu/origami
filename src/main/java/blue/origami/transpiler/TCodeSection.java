package blue.origami.transpiler;

import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TLetCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TParamCode;
import blue.origami.util.OLog;

public interface TCodeSection {
	public void push(String t);

	public void push(TCode t);

	public void pushLog(OLog log);

	public void pushBool(TBoolCode code);

	public void pushInt(TIntCode code);

	public void pushDouble(TDoubleCode code);

	public void pushCast(TCastCode code);

	public void pushName(TNameCode code);

	public void pushLet(TLetCode code);

	public void pushCall(TParamCode code);

}
