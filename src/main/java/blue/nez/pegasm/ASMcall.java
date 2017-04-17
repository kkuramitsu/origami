package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMcall extends PegAsmInstruction {
	public PegAsmInstruction jump = null;
	public String uname;

	public ASMcall(String uname, PegAsmInstruction next) {
		super(PegAsm.joinPoint(next));
		this.uname = uname;
	}

	public ASMcall(PegAsmInstruction jump, String uname, PegAsmInstruction next) {
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
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.xCall(this.uname, this.jump);
		return this.next;
	}

}