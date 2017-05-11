package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMjump extends PAsmInst {
	public PAsmInst jump = null;

	public ASMjump(PAsmInst jump) {
		super(null);
		this.jump = jump;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitJump(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		return this.jump;
	}

}