package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstByte;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMObyte extends AbstByte {
	public ASMObyte(int byteChar, PegAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitOByte(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() == this.byteChar) {
			if (this.byteChar == 0) {
				return this.next;
			}
			sc.move(1);
		}
		return this.next;
	}
}