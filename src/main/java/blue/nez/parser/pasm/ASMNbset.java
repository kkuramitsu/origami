package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public class ASMNbset extends PAsmInst {
	public final boolean[] bools;

	public ASMNbset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
		this.bools[0] = true;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNSet(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.getbyte();
		if (!this.bools[byteChar]) {
			return this.next;
		}
		return px.raiseFail();
	}

}