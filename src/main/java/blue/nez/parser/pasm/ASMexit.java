package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMexit extends PAsmInst {
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
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		throw new ParserTerminationException(this.status);
	}
}