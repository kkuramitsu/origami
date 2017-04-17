package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMNbin extends ASMNbyte {
	public ASMNbin(int byteChar, PegAsmInstruction next) {
		super(byteChar, next);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() != this.byteChar && !sc.eof()) {
			return this.next;
		}
		return sc.xFail();
	}
}