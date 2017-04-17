package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTfold extends PegAsmInst {
	public final int shift;
	public final Symbol label;

	public ASMTfold(Symbol label, int shift, PegAsmInst next) {
		super(next);
		this.label = label;
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTFold(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		px.foldTree(this.shift, this.label);
		return this.next;
	}
}