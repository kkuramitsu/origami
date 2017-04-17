package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstByte;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMNbyte extends AbstByte {
	public ASMNbyte(int byteChar, PegAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNByte(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() != this.byteChar) {
			return this.next;
		}
		return sc.raiseFail();
	}

}