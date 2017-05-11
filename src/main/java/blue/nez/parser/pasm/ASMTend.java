package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTend extends PAsmInst {
	public final int shift;
	public final Symbol tag;
	public final String value;

	public ASMTend(Symbol tag, String value, int shift, PAsmInst next) {
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
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		px.endTree(this.shift, this.tag, this.value);
		return this.next;
	}
}