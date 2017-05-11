package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmContext.StackData;
import blue.nez.parser.PAsmInst;

public final class ASMret extends PAsmInst {
	public ASMret() {
		super(null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRet(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.popStack();
		return (PAsmInst) s.ref;
	}

}