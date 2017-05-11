package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTbegin extends PAsmInst {
	public final int shift;

	public ASMTbegin(int shift, PAsmInst next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTBegin(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		px.beginTree(this.shift);
		return this.next;
	}
}