package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMnop extends PegAsmInst {
	public final String name;

	public ASMnop(String name, PegAsmInst next) {
		super(next);
		this.name = name;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNop(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return this.next;
	}

}