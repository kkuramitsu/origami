package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMexit extends PegAsmInst {
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
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		throw new ParserTerminationException(this.status);
	}
}