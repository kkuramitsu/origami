package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMstep extends PegAsmInst {
	public ASMstep() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitStep(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return sc.xStep(this.next);
	}
}