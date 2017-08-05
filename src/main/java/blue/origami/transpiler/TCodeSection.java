package blue.origami.transpiler;

import blue.origami.transpiler.code.TApplyCode;
import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDataCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TFuncCode;
import blue.origami.transpiler.code.TIfCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TLetCode;
import blue.origami.transpiler.code.TLogCode;
import blue.origami.transpiler.code.TMultiCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TReturnCode;
import blue.origami.transpiler.code.TStringCode;
import blue.origami.transpiler.code.TemplateCode;

public interface TCodeSection {
	public void push(String t);

	public void push(TCode t);

	public void pushBool(TEnv env, TBoolCode code);

	public void pushInt(TEnv env, TIntCode code);

	public void pushDouble(TEnv env, TDoubleCode code);

	public void pushString(TEnv env, TStringCode code);

	public void pushCast(TEnv env, TCastCode code);

	public void pushCall(TEnv env, TCode code);

	public void pushLet(TEnv env, TLetCode code);

	public void pushName(TEnv env, TNameCode code);

	public void pushIf(TEnv env, TIfCode code);

	public void pushReturn(TEnv env, TReturnCode code);

	public void pushMulti(TEnv env, TMultiCode code);

	public void pushTemplate(TEnv env, TemplateCode code);

	// public void pushArray(TEnv env, TArrayCode code);

	public void pushData(TEnv env, TDataCode code);

	public void pushFuncExpr(TEnv env, TFuncCode code);

	public void pushApply(TEnv env, TApplyCode code);

	public void pushError(TEnv env, TErrorCode code);

	public default void pushLog(TEnv env, TLogCode code) {
		env.reportLog(code.getLog());
		code.getInner().emitCode(env, this);
	}

}
