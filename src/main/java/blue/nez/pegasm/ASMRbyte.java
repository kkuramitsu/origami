package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstByte;

public class ASMRbyte extends AbstByte {
	public ASMRbyte(int byteChar, PegAsmInstruction next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRByte(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		while (sc.prefetch() == this.byteChar) {
			sc.move(1);
		}
		return this.next;
	}
}