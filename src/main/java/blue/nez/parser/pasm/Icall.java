package blue.nez.parser.pasm;

public class Icall extends PAsmInst {
	public PAsmInst jump = null;
	public String uname;

	public Icall(String uname, PAsmInst next) {
		super(next);
		this.uname = uname;
	}

	public final String getNonTerminalName() {
		return this.uname;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		pushRet(px, this.next);
		return this.jump;
	}

}