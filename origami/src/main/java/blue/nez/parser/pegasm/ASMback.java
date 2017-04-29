package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmContext.StackData;
import blue.nez.parser.PegAsmInst;

public final class ASMback extends PegAsmInst {
	public ASMback(PegAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitBack(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.popStack();
		px.backtrack(s.value);
		return this.next;
	}

}