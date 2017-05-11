package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMpos extends PAsmInst {
	public ASMpos(PAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitPos(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		sc.xPos();
		return this.next;
	}

}