package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMmove extends PegAsmInstruction {
	public final int shift;

	public ASMmove(int shift, PegAsmInstruction next) {
		super(next);
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitMove(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.move(this.shift);
		return this.next;
	}

}