package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstByte;

public class ASMNbyte extends AbstByte {
	public ASMNbyte(int byteChar, PegAsmInstruction next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNByte(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() != this.byteChar) {
			return this.next;
		}
		return sc.xFail();
	}

}