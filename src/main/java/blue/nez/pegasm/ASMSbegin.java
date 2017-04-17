package blue.nez.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmContext.StackData;
import blue.nez.parser.PegAsmInstruction;

public final class ASMSbegin extends PegAsmInstruction {
	public ASMSbegin(PegAsmInstruction next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSOpen(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.newUnusedStack();
		s.ref = px.loadSymbolTable();
		return this.next;
	}

}