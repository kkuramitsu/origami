package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public final class ASMbinset extends PegAsmInst {
	public final boolean[] bools;

	public ASMbinset(boolean[] bools, PegAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.prefetch();
		if (this.bools[byteChar] && !px.eof()) {
			px.move(1);
			return this.next;
		}
		return px.raiseFail();
	}

	@Override
	public void visit(PegAsmVisitor v) {
		// TODO Auto-generated method stub

	}

}