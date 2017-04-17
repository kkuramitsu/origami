package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstByte;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMbyte extends AbstByte {
	public ASMbyte(int byteChar, PegAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitByte(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		if (px.read() == this.byteChar) {
			return this.next;
		}
		return px.raiseFail();
	}
}