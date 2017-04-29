package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public class ASMbset extends PegAsmInst {
	public final boolean[] bools;

	public ASMbset(boolean[] bools, PegAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSet(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.read();
		if (this.bools[byteChar]) {
			return this.next;
		}
		return px.raiseFail();
	}

}