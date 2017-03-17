package origami.code;

public interface OGenerator {

	public void pushMulti(OMultiCode node);

	public void pushReturn(OReturnCode node);

	public void pushIf(OIfCode node);

	public void pushValue(OValueCode node);

	public void pushArray(OArrayCode node);

	public void pushName(ONameCode node);

	public void pushAssign(OAssignCode node);

	public void pushConstructor(OConstructorCode node);

	public void pushMethod(OMethodCode node);

	public void pushCast(OCastCode node);

	public void pushSetter(OSetterCode node);

	public void pushGetter(GetterCode node);

	// public void pushDynamic(CallSiteCode node);

	public void pushUndefined(OCode node);

	public void pushError(OErrorCode node);

	public void pushWarning(OWarningCode node);

	/*--------*/

	public void pushThis();

	public void pushSetIndex(SetIndexCode code);

	public void pushGetIndex(GetIndexCode code);

	public void pushThrow(OThrowCode code);

	public void pushBreak(OBreakCode code);

	public void pushContinue(OContinueCode code);

	public void pushBlockCode(OLabelBlockCode code);

	public void pushLoop(ForCode code);

	public void pushTry(TryCatchCode code);

	public void pushSwitch(SwitchCode code);

}
