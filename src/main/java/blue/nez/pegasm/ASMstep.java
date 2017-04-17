package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMstep extends PegAsmInstruction {
	public ASMstep() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitStep(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return sc.xStep(this.next);
	}
}