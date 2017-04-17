package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTpush extends PegAsmInst {
	public ASMTpush(PegAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTPush(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		px.pushTree();
		return this.next;
	}

}