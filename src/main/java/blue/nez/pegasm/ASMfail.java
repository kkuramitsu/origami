package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMfail extends PegAsmInstruction {
	public ASMfail() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitFail(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return sc.xFail();
	}

}