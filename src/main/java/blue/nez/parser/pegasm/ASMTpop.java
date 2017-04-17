package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTpop extends PegAsmInst {

	public ASMTpop(PegAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTPop(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		px.popTree();
		return this.next;
	}

}