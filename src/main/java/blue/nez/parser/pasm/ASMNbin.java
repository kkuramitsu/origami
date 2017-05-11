package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMNbin extends ASMNbyte {
	public ASMNbin(int byteChar, PAsmInst next) {
		super(byteChar, next);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		if (sc.getbyte() != this.byteChar && !sc.eof()) {
			return this.next;
		}
		return sc.raiseFail();
	}
}