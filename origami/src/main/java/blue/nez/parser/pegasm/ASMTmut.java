package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTmut extends PegAsmInst {
	public final String value;

	public ASMTmut(String value, PegAsmInst next) {
		super(next);
		this.value = value;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTReplace(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		px.valueTree(this.value);
		return this.next;
	}

}