package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMbin extends ASMbyte {
	public ASMbin(PegAsmInst next) {
		super(0, next);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		if (px.prefetch() == 0 && !px.eof()) {
			px.move(1);
			return this.next;
		}
		return px.raiseFail();
	}
}