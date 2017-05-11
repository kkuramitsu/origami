package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmContext.StackData;
import blue.nez.parser.PAsmInst;

public final class ASMback extends PAsmInst {
	public ASMback(PAsmInst next) {
		super(next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitBack(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		StackData s = px.popStack();
		px.backtrack(s.value);
		return this.next;
	}

}