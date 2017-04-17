package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMObin extends ASMObyte {
	public ASMObin(PegAsmInstruction next) {
		super(0, next);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (sc.prefetch() == 0 && !sc.eof()) {
			sc.move(1);
		}
		return this.next;
	}
}