package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMRbinset extends ASMRbset {
	public ASMRbinset(boolean[] byteMap, PegAsmInstruction next) {
		super(byteMap, next);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		while (this.byteSet[sc.prefetch()] && !sc.eof()) {
			sc.move(1);
		}
		return this.next;
	}

}