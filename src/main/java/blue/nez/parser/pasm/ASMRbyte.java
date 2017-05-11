package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.pasm.PegAsm.AbstByte;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMRbyte extends AbstByte {
	public ASMRbyte(int byteChar, PAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRByte(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		while (sc.getbyte() == this.byteChar) {
			sc.move(1);
		}
		return this.next;
	}
}