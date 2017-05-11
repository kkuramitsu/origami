package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMRbin extends ASMRbyte {
	public ASMRbin(PAsmInst next) {
		super(0, next);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		while (sc.getbyte() == 0 && !sc.eof()) {
			sc.move(1);
		}
		return this.next;
	}
}