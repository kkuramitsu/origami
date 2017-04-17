package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstAny;

public final class ASMany extends AbstAny {
	public ASMany(PegAsmInstruction next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitAny(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (!sc.eof()) {
			sc.move(1);
			return this.next;
		}
		return sc.xFail();
	}
}