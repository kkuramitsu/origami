package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.pasm.PegAsm.AbstByte;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMbyte extends AbstByte {
	public ASMbyte(int byteChar, PAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitByte(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		if (px.nextbyte() == this.byteChar) {
			return this.next;
		}
		return px.raiseFail();
	}
}