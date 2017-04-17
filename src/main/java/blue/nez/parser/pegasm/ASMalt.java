package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmContext.StackData;
import blue.nez.parser.PegAsmInst;

public final class ASMalt extends PegAsmInst {
	public final PegAsmInst jump; // jump if failed

	public ASMalt(PegAsmInst failjump, PegAsmInst next) {
		super(next);
		this.jump = PegAsm.joinPoint(failjump);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitAlt(this);
	}

	@Override
	public PegAsmInst branch() {
		return this.jump;
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
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