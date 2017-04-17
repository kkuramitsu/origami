package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmContext.StackData;
import blue.nez.parser.PegAsmInst;

public class ASMcall extends PegAsmInst {
	public PegAsmInst jump = null;
	public String uname;

	public ASMcall(String uname, PegAsmInst next) {
		super(PegAsm.joinPoint(next));
		this.uname = uname;
	}

	public ASMcall(PegAsmInst jump, String uname, PegAsmInst next) {
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
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.newUnusedStack();
		s.ref = this.jump;
		return this.next;
	}

}