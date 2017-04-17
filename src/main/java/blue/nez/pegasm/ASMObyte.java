package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstByte;

public class ASMObyte extends AbstByte {
	public ASMObyte(int byteChar, PegAsmInstruction next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitOByte(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() == this.byteChar) {
			if (this.byteChar == 0) {
				return this.next;
			}
			sc.move(1);
		}
		return this.next;
	}
}