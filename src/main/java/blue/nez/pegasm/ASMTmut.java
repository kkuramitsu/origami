package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTmut extends PegAsmInstruction {
	public final String value;

	public ASMTmut(String value, PegAsmInstruction next) {
		super(next);
		this.value = value;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTReplace(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.valueTree(this.value);
		return this.next;
	}

}