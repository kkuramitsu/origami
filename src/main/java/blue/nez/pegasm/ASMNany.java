package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstAny;

public final class ASMNany extends AbstAny {

	public ASMNany(PegAsmInstruction next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNAny(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.eof()) {
			return this.next;
		}
		return sc.xFail();
	}
}