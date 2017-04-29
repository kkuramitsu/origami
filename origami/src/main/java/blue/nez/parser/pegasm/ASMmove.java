package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMmove extends PegAsmInst {
	public final int shift;

	public ASMmove(int shift, PegAsmInst next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitMove(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.move(this.shift);
		return this.next;
	}

}