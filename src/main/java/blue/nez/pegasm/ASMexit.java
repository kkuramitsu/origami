package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMexit extends PegAsmInstruction {
	public final boolean status;

	public ASMexit(boolean status) {
		super(null);
		this.status = status;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitExit(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		throw new ParserTerminationException(this.status);
	}
}