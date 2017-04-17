package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTemit extends PegAsmInstruction {
	public final Symbol label;

	public ASMTemit(Symbol label, PegAsmInstruction next) {
		super(next);
		this.label = label;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTEmit(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return this.next;
	}

}