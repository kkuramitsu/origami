package blue.nez.parser.pasm;

public class ASMbset extends PAsmInst {
	public final boolean[] bools;

	public ASMbset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return (this.bools[nextbyte(px)]) ? this.next : raiseFail(px);
	}

}