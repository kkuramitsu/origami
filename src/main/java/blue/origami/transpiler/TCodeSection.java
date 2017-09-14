package blue.origami.transpiler;

import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.CallCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.ExistFieldCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.GroupCode;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.LogCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.code.NoneCode;
import blue.origami.transpiler.code.ReturnCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.transpiler.code.TemplateCode;

public interface TCodeSection {

	public void pushNone(TEnv env, NoneCode code);

	public void pushBool(TEnv env, BoolCode code);

	public void pushInt(TEnv env, IntCode code);

	public void pushDouble(TEnv env, DoubleCode code);

	public void pushString(TEnv env, StringCode code);

	public void pushCast(TEnv env, CastCode code);

	public void pushCall(TEnv env, CallCode code);

	public void pushLet(TEnv env, LetCode code);

	public void pushName(TEnv env, NameCode code);

	public void pushIf(TEnv env, IfCode code);

	public void pushReturn(TEnv env, ReturnCode code);

	public void pushMulti(TEnv env, MultiCode code);

	public void pushTemplate(TEnv env, TemplateCode code);

	// public void pushArray(TEnv env, TArrayCode code);

	public void pushData(TEnv env, DataCode code);

	public void pushFuncExpr(TEnv env, FuncCode code);

	public void pushApply(TEnv env, ApplyCode code);

	public void pushError(TEnv env, ErrorCode code);

	public default void pushLog(TEnv env, LogCode code) {
		env.reportLog(code.getLog());
		code.getInner().emitCode(env, this);
	}

	public void pushFuncRef(TEnv env, FuncRefCode code);

	public void pushGet(TEnv env, GetCode code);

	public void pushSet(TEnv env, SetCode code);

	public void pushExistField(TEnv env, ExistFieldCode code);

	public void pushGroup(TEnv env, GroupCode code);

}
