package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmContext.StackData;
import blue.nez.parser.PAsmInst;

public final class ASMSbegin extends PAsmInst {
	public ASMSbegin(PAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSOpen(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.newUnusedStack();
		s.ref = px.loadSymbolTable();
		return this.next;
	}

}