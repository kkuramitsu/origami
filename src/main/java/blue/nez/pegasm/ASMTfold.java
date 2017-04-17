package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTfold extends PegAsmInstruction {
	public final int shift;
	public final Symbol label;

	public ASMTfold(Symbol label, int shift, PegAsmInstruction next) {
		super(next);
		this.label = label;
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTFold(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.foldTree(this.shift, this.label);
		return this.next;
	}
}