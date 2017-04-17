package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstAny;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMNany extends AbstAny {

	public ASMNany(PegAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNAny(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.eof()) {
			return this.next;
		}
		return sc.raiseFail();
	}
}