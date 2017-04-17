package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTend extends PegAsmInstruction {
	public final int shift;
	public final Symbol tag;
	public final String value;

	public ASMTend(Symbol tag, String value, int shift, PegAsmInstruction next) {
		super(next);
		this.tag = tag;
		this.value = value;
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTEnd(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.endTree(this.shift, this.tag, this.value);
		return this.next;
	}
}