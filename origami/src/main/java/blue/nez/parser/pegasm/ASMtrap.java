package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMtrap extends PegAsmInst {
	public final int type;
	public final int uid;

	public ASMtrap(int type, int uid, PegAsmInst next) {
		super(next);
		this.type = type;
		this.uid = uid;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTrap(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.trap(this.type, this.uid);
		return this.next;
	}
}