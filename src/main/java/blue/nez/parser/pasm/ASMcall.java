package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmContext.StackData;
import blue.nez.parser.PAsmInst;

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
	public void visit(PegAsmVisitor v) {
		v.visitCall(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.newUnusedStack();
		s.ref = this.jump;
		return this.next;
	}

}