package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmContext.StackData;
import blue.nez.parser.PegAsmInst;

public final class ASMSend extends PegAsmInst {
	public ASMSend(PegAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSClose(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.popStack();
		px.storeSymbolTable(s.ref);
		return this.next;
	}
}