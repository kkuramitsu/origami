package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public final class ASMNbinset extends PAsmInst {
	public final boolean[] bools;

	public ASMNbinset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.getbyte();
		if (!this.bools[byteChar] && !px.eof()) {
			return this.next;
		}
		return px.raiseFail();
	}

	@Override
	public void visit(PegAsmVisitor v) {
		// TODO Auto-generated method stub

	}

}