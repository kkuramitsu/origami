package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public class ASMObset extends PAsmInst {
	public final boolean[] bools;

	public ASMObset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitOSet(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.getbyte();
		if (this.bools[byteChar]) {
			px.move(1);
		}
		return this.next;
	}

}