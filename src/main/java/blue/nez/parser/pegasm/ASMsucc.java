package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMsucc extends PegAsmInst {
	public ASMsucc(PegAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSucc(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.xSucc();
		return this.next;
	}
}