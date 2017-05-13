package blue.nez.parser.pasm;

public class ASMcall extends PAsmInst {
	public PAsmInst jump = null;
	public String uname;

	public ASMcall(String uname, PAsmInst next) {
		super(PegAsm.joinPoint(next));
		this.uname = uname;
	}

	public ASMcall(PAsmInst jump, String uname, PAsmInst next) {
		super(PegAsm.joinPoint(jump));
		this.uname = uname;
		this.jump = PegAsm.joinPoint(next);
	}

	public final String getNonTerminalName() {
		return this.uname;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushRet(px, this.jump);
		return this.next;
	}

}