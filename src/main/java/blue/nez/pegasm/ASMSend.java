package blue.nez.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmContext.StackData;
import blue.nez.parser.PegAsmInstruction;

public final class ASMSend extends PegAsmInstruction {
	public ASMSend(PegAsmInstruction next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSClose(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.popStack();
		px.storeSymbolTable(s.ref);
		return this.next;
	}
}