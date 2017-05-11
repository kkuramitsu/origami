package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.pasm.PegAsm.AbstByte;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMNbyte extends AbstByte {
	public ASMNbyte(int byteChar, PAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNByte(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		if (sc.getbyte() != this.byteChar) {
			return this.next;
		}
		return sc.raiseFail();
	}

}