package blue.origami.transpiler;

import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TIfCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TLetCode;
import blue.origami.transpiler.code.TMultiCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TReturnCode;
import blue.origami.util.OLog;

public interface TCodeSection {
	public void push(String t);

	public void push(TCode t);

	public void pushLog(OLog log);

	public void pushBool(TEnv env, TBoolCode code);

	public void pushInt(TEnv env, TIntCode code);

	public void pushDouble(TEnv env, TDoubleCode code);

	public void pushCast(TEnv env, TCastCode code);

	public void pushCall(TEnv env, TCode code);

	public void pushLet(TEnv env, TLetCode code);

	public void pushName(TEnv env, TNameCode code);

	public void pushIf(TEnv env, TIfCode code);

	public void pushReturn(TEnv env, TReturnCode code);

	public void pushMulti(TEnv env, TMultiCode tMultiCode);

}
