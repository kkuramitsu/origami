package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMRbin extends ASMRbyte {
	public ASMRbin(PegAsmInst next) {
		super(0, next);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		while (sc.prefetch() == 0 && !sc.eof()) {
			sc.move(1);
		}
		return this.next;
	}
}