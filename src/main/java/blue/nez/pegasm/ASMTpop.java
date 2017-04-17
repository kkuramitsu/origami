package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTpop extends PegAsmInstruction {

	public ASMTpop(PegAsmInstruction next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTPop(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.xTPop();
		return this.next;
	}

}