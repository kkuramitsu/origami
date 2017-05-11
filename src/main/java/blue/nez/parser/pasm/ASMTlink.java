package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTlink extends PAsmInst {
	public final Symbol label;

	public ASMTlink(Symbol label, PAsmInst next) {
		super(next);
		this.label = label;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTLink(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		px.linkTree(this.label);
		return this.next;
	}

}