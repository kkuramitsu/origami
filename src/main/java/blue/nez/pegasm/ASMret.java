package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMret extends PegAsmInstruction {
	public ASMret() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRet(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return sc.xRet();
	}

}