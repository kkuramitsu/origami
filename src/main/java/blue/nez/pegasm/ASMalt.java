package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMalt extends PegAsmInstruction {
	public final PegAsmInstruction jump;

	public ASMalt(PegAsmInstruction failjump, PegAsmInstruction next) {
		super(next);
		this.jump = PegAsm.joinPoint(failjump);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitAlt(this);
	}

	@Override
	public PegAsmInstruction branch() {
		return this.jump;
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.xAlt(this.jump);
		return this.next;
	}
}