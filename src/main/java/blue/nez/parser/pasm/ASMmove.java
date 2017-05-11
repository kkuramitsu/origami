package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMmove extends PAsmInst {
	public final int shift;

	public ASMmove(int shift, PAsmInst next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitMove(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		sc.move(this.shift);
		return this.next;
	}

}