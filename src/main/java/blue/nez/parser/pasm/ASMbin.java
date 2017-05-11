package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMbin extends ASMbyte {
	public ASMbin(PAsmInst next) {
		super(0, next);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		if (px.getbyte() == 0 && !px.eof()) {
			px.move(1);
			return this.next;
		}
		return px.raiseFail();
	}
}