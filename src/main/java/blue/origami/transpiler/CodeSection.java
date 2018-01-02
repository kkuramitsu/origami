package blue.origami.transpiler;

import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.AssignCode;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.BreakCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CharCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.DictCode;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.GroupCode;
import blue.origami.transpiler.code.HasCode;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.ListCode;
import blue.origami.transpiler.code.LogCode;
import blue.origami.transpiler.code.MappedCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.RangeCode;
import blue.origami.transpiler.code.ReturnCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.transpiler.code.SwitchCode;
import blue.origami.transpiler.code.TemplateCode;
import blue.origami.transpiler.code.ThrowCode;
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.code.TupleIndexCode;
import blue.origami.transpiler.code.VarNameCode;
import blue.origami.transpiler.code.WhileCode;

public interface CodeSection {

	public Env env();

	public void pushNone(Code code);

	public void pushBool(BoolCode code);

	public void pushInt(IntCode code);

	public void pushDouble(DoubleCode code);

	public void pushString(StringCode code);

	public void pushChar(CharCode code);

	public void pushCast(CastCode code);

	public void pushCall(MappedCode code);

	public void pushLet(LetCode code);

	public void pushName(VarNameCode code);

	public void pushIf(IfCode code);

	public void pushReturn(ReturnCode code);

	public void pushMulti(MultiCode code);

	public void pushTemplate(TemplateCode code);

	// public void pushArray(TEnv env, TArrayCode code);

	public void pushTuple(TupleCode code);

	public void pushTupleIndex(TupleIndexCode code);

	public void pushData(DataCode code);

	public void pushList(ListCode code);

	public void pushRange(RangeCode code);

	public void pushDict(DictCode dictCode);

	public void pushFuncExpr(FuncCode code);

	public void pushApply(ApplyCode code);

	public void pushError(ErrorCode code);

	public default void pushLog(LogCode code) {
		env().reportLog(code.getLog());
		code.getInner().emitCode(this);
	}

	public void pushFuncRef(FuncRefCode code);

	public void pushGet(GetCode code);

	public void pushSet(SetCode code);

	public void pushHas(HasCode code);

	public void pushGroup(GroupCode code);

	// imperative programming

	public void pushAssign(AssignCode code);

	public void pushWhile(WhileCode code);

	public void pushBreak(BreakCode code);

	public void pushThrow(ThrowCode code);

	public void pushSwitch(SwitchCode code);

}
