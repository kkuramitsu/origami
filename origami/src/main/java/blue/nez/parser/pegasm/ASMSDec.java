package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMSDec extends PegAsmInst {
	public final PegAsmInst jump;

	public ASMSDec(PegAsmInst jump, PegAsmInst next) {
		super(next);
		this.jump = jump;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNDec(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return sc.decCount() ? this.next : this.jump;
	}
}