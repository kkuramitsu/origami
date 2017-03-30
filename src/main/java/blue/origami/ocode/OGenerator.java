package blue.origami.ocode;

public interface OGenerator {

	public void push(OCode node);

	public void pushValue(ValueCode node);

	public void pushArray(ArrayCode node);

	public void pushLambda(LambdaCode node);

	public void pushName(NameCode node);

	public void pushConstructor(NewCode node);

	public void pushMethod(ApplyCode node);

	public void pushCast(CastCode node);

	public void pushSetter(SetterCode node);

	public void pushGetter(GetterCode node);

	public void pushInstanceOf(InstanceOfCode code);

	public void pushAnd(AndCode code);

	public void pushOr(OrCode code);

	public void pushNot(NotCode code);

	public void pushGetSize(GetSizeCode code);

	public void pushSetIndex(SetIndexCode code);

	public void pushGetIndex(GetIndexCode code);

	public void pushMulti(MultiCode node);

	public void pushAssign(AssignCode node);

	public void pushIf(IfCode node);

	public void pushWhile(WhileCode code);

	public void pushTry(TryCode code);

	public void pushReturn(ReturnCode node);

	public void pushThrow(ThrowCode code);

	public void pushBreak(BreakCode code);

	public void pushContinue(ContinueCode code);

	// public void pushBlockCode(OLabelBlockCode code);

	/*--------*/

	public void pushThis();

	// public void pushLoop(ForCode code);

	public void pushSugar(SugarCode oSugarCode);
	// public void pushUndefined(OCode node);

	public void pushError(ErrorCode node);

	public void pushWarning(WarningCode node);

}
