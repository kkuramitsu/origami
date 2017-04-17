package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTlink extends PegAsmInstruction {
	public final Symbol label;

	public ASMTlink(Symbol label, PegAsmInstruction next) {
		super(next);
		this.label = label;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTLink(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.xTLink(this.label);
		return this.next;
	}

}