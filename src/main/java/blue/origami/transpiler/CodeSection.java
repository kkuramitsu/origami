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
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.code.TupleIndexCode;

public interface CodeSection {

	public void pushNone(Env env, NoneCode code);

	public void pushBool(Env env, BoolCode code);

	public void pushInt(Env env, IntCode code);

	public void pushDouble(Env env, DoubleCode code);

	public void pushString(Env env, StringCode code);

	public void pushCast(Env env, CastCode code);

	public void pushCall(Env env, CallCode code);

	public void pushLet(Env env, LetCode code);

	public void pushName(Env env, NameCode code);

	public void pushIf(Env env, IfCode code);

	public void pushReturn(Env env, ReturnCode code);

	public void pushMulti(Env env, MultiCode code);

	public void pushTemplate(Env env, TemplateCode code);

	// public void pushArray(TEnv env, TArrayCode code);

	public void pushTuple(Env env, TupleCode code);

	public void pushTupleIndex(Env env, TupleIndexCode code);

	public void pushData(Env env, DataCode code);

	public void pushFuncExpr(Env env, FuncCode code);

	public void pushApply(Env env, ApplyCode code);

	public void pushError(Env env, ErrorCode code);

	public default void pushLog(Env env, LogCode code) {
		env.reportLog(code.getLog());
		code.getInner().emitCode(env, this);
	}

	public void pushFuncRef(Env env, FuncRefCode code);

	public void pushGet(Env env, GetCode code);

	public void pushSet(Env env, SetCode code);

	public void pushExistField(Env env, ExistFieldCode code);

	public void pushGroup(Env env, GroupCode code);

}
