package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMtrap extends PAsmInst {
	public final int type;
	public final int uid;

	public ASMtrap(int type, int uid, PAsmInst next) {
		super(next);
		this.type = type;
		this.uid = uid;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTrap(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		sc.trap(this.type, this.uid);
		return this.next;
	}
}