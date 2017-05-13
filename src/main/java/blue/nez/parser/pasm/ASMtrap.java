package blue.nez.parser.pasm;

public final class ASMtrap extends PAsmInst {
	public final int type;
	public final int uid;

	public ASMtrap(int type, int uid, PAsmInst next) {
		super(next);
		this.type = type;
		this.uid = uid;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		px.trap(this.type, this.uid);
		return this.next;
	}
}