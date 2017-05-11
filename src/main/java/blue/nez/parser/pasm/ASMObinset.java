package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMObinset extends ASMObset {
	public ASMObinset(boolean[] byteMap, PAsmInst next) {
		super(byteMap, next);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		int byteChar = sc.getbyte();
		if (this.bools[byteChar] && sc.eof()) {
			sc.move(1);
		}
		return this.next;
	}
}