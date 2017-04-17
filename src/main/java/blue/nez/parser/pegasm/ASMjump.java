package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMjump extends PegAsmInst {
	public PegAsmInst jump = null;

	public ASMjump(PegAsmInst jump) {
		super(null);
		this.jump = jump;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitJump(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		return this.jump;
	}

}