package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTend extends PegAsmInst {
	public final int shift;
	public final Symbol tag;
	public final String value;

	public ASMTend(Symbol tag, String value, int shift, PegAsmInst next) {
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
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		px.endTree(this.shift, this.tag, this.value);
		return this.next;
	}
}