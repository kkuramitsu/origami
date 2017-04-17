package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMjump extends PegAsmInstruction {
	public PegAsmInstruction jump = null;

	public ASMjump(PegAsmInstruction jump) {
		super(null);
		this.jump = jump;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitJump(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return this.jump;
	}

}