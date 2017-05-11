package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.pasm.PegAsm.AbstByte;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMObyte extends AbstByte {
	public ASMObyte(int byteChar, PAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitOByte(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		if (sc.getbyte() == this.byteChar) {
			if (this.byteChar == 0) {
				return this.next;
			}
			sc.move(1);
		}
		return this.next;
	}
}