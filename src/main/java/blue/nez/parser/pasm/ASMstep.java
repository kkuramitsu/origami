package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMstep extends PAsmInst {
	public ASMstep() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitStep(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		return sc.xStep(this.next);
	}
}