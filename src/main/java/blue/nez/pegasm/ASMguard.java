package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMguard extends PegAsmInstruction {
	public ASMguard() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitGuard(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return sc.xStep(this.next);
	}
}