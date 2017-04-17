package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMtrap extends PegAsmInstruction {
	public final int type;
	public final int uid;

	public ASMtrap(int type, int uid, PegAsmInstruction next) {
		super(next);
		this.type = type;
		this.uid = uid;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTrap(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.trap(this.type, this.uid);
		return this.next;
	}
}