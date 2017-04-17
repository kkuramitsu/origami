package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstSet;

public class ASMObset extends AbstSet {
	public ASMObset(boolean[] byteMap, PegAsmInstruction next) {
		super(byteMap, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitOSet(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int byteChar = sc.prefetch();
		if (this.byteSet[byteChar]) {
			sc.move(1);
		}
		return this.next;
	}

}