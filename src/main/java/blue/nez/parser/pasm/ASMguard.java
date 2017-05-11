package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMguard extends PAsmInst {
	public ASMguard() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitGuard(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		return px.xStep(this.next);
	}
}