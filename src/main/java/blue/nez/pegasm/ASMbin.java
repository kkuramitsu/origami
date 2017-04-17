package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMbin extends ASMbyte {
	public ASMbin(PegAsmInstruction next) {
		super(0, next);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() == 0 && !sc.eof()) {
			sc.move(1);
			return this.next;
		}
		return sc.xFail();
	}
}