package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstByte;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMRbyte extends AbstByte {
	public ASMRbyte(int byteChar, PegAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRByte(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		while (sc.prefetch() == this.byteChar) {
			sc.move(1);
		}
		return this.next;
	}
}