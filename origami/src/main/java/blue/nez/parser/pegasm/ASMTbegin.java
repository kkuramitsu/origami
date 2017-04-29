package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTbegin extends PegAsmInst {
	public final int shift;

	public ASMTbegin(int shift, PegAsmInst next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTBegin(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		px.beginTree(this.shift);
		return this.next;
	}
}