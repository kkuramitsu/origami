package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.pasm.PegAsm.AbstAny;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMNany extends AbstAny {

	public ASMNany(PAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNAny(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		if (sc.eof()) {
			return this.next;
		}
		return sc.raiseFail();
	}
}