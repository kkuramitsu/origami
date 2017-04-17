package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMguard extends PegAsmInst {
	public ASMguard() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitGuard(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		return px.xStep(this.next);
	}
}