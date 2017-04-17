package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstByte;

public class ASMbyte extends AbstByte {
	public ASMbyte(int byteChar, PegAsmInstruction next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitByte(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.read() == this.byteChar) {
			return this.next;
		}
		return sc.xFail();
	}
}