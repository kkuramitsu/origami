package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public class ASMbset extends PAsmInst {
	public final boolean[] bools;

	public ASMbset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSet(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.nextbyte();
		if (this.bools[byteChar]) {
			return this.next;
		}
		return px.raiseFail();
	}

}