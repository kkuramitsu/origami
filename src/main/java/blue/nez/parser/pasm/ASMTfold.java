package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTfold extends PAsmInst {
	public final int shift;
	public final Symbol label;

	public ASMTfold(Symbol label, int shift, PAsmInst next) {
		super(next);
		this.label = label;
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTFold(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		px.foldTree(this.shift, this.label);
		return this.next;
	}
}