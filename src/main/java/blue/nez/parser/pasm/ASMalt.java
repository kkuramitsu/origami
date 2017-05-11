package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmContext.StackData;
import blue.nez.parser.PAsmInst;

public final class ASMalt extends PAsmInst {
	public final PAsmInst jump; // jump if failed

	public ASMalt(PAsmInst failjump, PAsmInst next) {
		super(next);
		this.jump = PegAsm.joinPoint(failjump);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitAlt(this);
	}

	@Override
	public PAsmInst branch() {
		return this.jump;
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		StackData s0 = px.newUnusedStack();
		StackData s1 = px.newUnusedStack();
		StackData s2 = px.newUnusedStack();
		s0.value = px.loadCatchPoint();
		s0.ref = px.left;
		s1.value = px.pos;
		s1.ref = this.jump;
		s2.value = px.loadTreeLog();
		s2.ref = px.loadSymbolTable();
		return this.next;
	}

}