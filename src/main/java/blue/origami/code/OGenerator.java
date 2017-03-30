package blue.origami.code;

public interface OGenerator {

	public void push(OCode node);

	public void pushValue(OValueCode node);

	public void pushArray(OArrayCode node);

	public void pushLambda(OLambdaCode node);

	public void pushName(ONameCode node);

	public void pushConstructor(OConstructorCode node);

	public void pushMethod(OMethodCode node);

	public void pushCast(OCastCode node);

	public void pushSetter(OSetterCode node);

	public void pushGetter(OGetterCode node);

	public void pushInstanceOf(OInstanceOfCode code);

	public void pushAnd(OAndCode code);

	public void pushOr(OOrCode code);

	public void pushNot(ONotCode code);

	public void pushGetSize(OGetSizeCode code);

	public void pushSetIndex(OSetIndexCode code);

	public void pushGetIndex(OGetIndexCode code);

	public void pushMulti(OMultiCode node);

	public void pushAssign(OAssignCode node);

	public void pushIf(OIfCode node);

	public void pushWhile(OWhileCode code);

	public void pushTry(OTryCode code);

	public void pushReturn(OReturnCode node);

	public void pushThrow(OThrowCode code);

	public void pushBreak(OBreakCode code);

	public void pushContinue(OContinueCode code);

	// public void pushBlockCode(OLabelBlockCode code);

	/*--------*/

	public void pushThis();

	// public void pushLoop(ForCode code);

	public void pushSugar(OSugarCode oSugarCode);
	// public void pushUndefined(OCode node);

	public void pushError(OErrorCode node);

	public void pushWarning(OWarningCode node);

}
