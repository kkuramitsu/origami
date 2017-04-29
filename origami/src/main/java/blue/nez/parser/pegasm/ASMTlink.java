package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTlink extends PegAsmInst {
	public final Symbol label;

	public ASMTlink(Symbol label, PegAsmInst next) {
		super(next);
		this.label = label;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTLink(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		px.linkTree(this.label);
		return this.next;
	}

}