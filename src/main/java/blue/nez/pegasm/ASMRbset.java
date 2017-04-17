package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstSet;

public class ASMRbset extends AbstSet {
	public ASMRbset(boolean[] byteMap, PegAsmInstruction next) {
		super(byteMap, next);
		byteMap[0] = false;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRSet(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		while (this.byteSet[sc.prefetch()]) {
			sc.move(1);
		}
		return this.next;
	}

}