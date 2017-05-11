package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMnop extends PAsmInst {
	public final String name;

	public ASMnop(String name, PAsmInst next) {
		super(next);
		this.name = name;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNop(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		return this.next;
	}

}