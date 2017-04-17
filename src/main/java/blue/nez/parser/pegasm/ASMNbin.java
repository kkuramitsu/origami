package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMNbin extends ASMNbyte {
	public ASMNbin(int byteChar, PegAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() != this.byteChar && !sc.eof()) {
			return this.next;
		}
		return sc.raiseFail();
	}
}