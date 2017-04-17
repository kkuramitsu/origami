package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMpos extends PegAsmInstruction {
	public ASMpos(PegAsmInstruction next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitPos(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.xPos();
		return this.next;
	}

}