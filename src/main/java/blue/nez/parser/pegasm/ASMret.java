package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmContext.StackData;
import blue.nez.parser.PegAsmInst;

public final class ASMret extends PegAsmInst {
	public ASMret() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRet(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.popStack();
		return (PegAsmInst) s.ref;
	}

}