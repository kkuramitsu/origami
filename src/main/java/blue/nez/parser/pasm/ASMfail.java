package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMfail extends PAsmInst {
	public ASMfail() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitFail(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		return px.raiseFail();
	}

}