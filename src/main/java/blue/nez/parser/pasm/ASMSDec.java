package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMSDec extends PAsmInst {
	public final PAsmInst jump;

	public ASMSDec(PAsmInst jump, PAsmInst next) {
		super(next);
		this.jump = jump;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNDec(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		return sc.decCount() ? this.next : this.jump;
	}
}