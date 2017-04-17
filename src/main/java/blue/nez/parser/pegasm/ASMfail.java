package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMfail extends PegAsmInst {
	public ASMfail() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitFail(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		return px.raiseFail();
	}

}