package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMObinset extends ASMObset {
	public ASMObinset(boolean[] byteMap, PegAsmInst next) {
		super(byteMap, next);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int byteChar = sc.prefetch();
		if (this.bools[byteChar] && sc.eof()) {
			sc.move(1);
		}
		return this.next;
	}
}