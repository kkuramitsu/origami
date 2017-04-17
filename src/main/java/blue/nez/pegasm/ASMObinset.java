package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMObinset extends ASMObset {
	public ASMObinset(boolean[] byteMap, PegAsmInstruction next) {
		super(byteMap, next);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int byteChar = sc.prefetch();
		if (this.byteSet[byteChar] && sc.eof()) {
			sc.move(1);
		}
		return this.next;
	}
}