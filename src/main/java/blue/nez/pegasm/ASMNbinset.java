package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMNbinset extends ASMNbset {
	public ASMNbinset(boolean[] byteMap, PegAsmInstruction next) {
		super(byteMap, next);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int byteChar = sc.prefetch();
		if (!this.byteSet[byteChar] && !sc.eof()) {
			return this.next;
		}
		return sc.xFail();
	}

}