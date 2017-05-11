package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTmut extends PAsmInst {
	public final String value;

	public ASMTmut(String value, PAsmInst next) {
		super(next);
		this.value = value;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTReplace(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		px.valueTree(this.value);
		return this.next;
	}

}