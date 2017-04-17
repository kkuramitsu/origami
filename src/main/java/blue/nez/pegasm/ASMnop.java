package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMnop extends PegAsmInstruction {
	public final String name;

	public ASMnop(String name, PegAsmInstruction next) {
		super(next);
		this.name = name;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNop(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return this.next;
	}

}