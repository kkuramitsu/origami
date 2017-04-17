package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMSDec extends PegAsmInstruction {
	public final PegAsmInstruction jump;

	public ASMSDec(PegAsmInstruction jump, PegAsmInstruction next) {
		super(next);
		this.jump = jump;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNDec(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return sc.decCount() ? this.next : this.jump;
	}
}