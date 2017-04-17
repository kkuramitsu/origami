package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public class ASMNbset extends PegAsmInst {
	public final boolean[] bools;

	public ASMNbset(boolean[] bools, PegAsmInst next) {
		super(next);
		this.bools = bools;
		this.bools[0] = true;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNSet(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.prefetch();
		if (!this.bools[byteChar]) {
			return this.next;
		}
		return px.raiseFail();
	}

}