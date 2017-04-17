package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstSet;

public class ASMNbset extends AbstSet {
	public ASMNbset(boolean[] byteMap, PegAsmInstruction next) {
		super(byteMap, next);
		this.byteSet[0] = true;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNSet(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int byteChar = sc.prefetch();
		if (!this.byteSet[byteChar]) {
			return this.next;
		}
		return sc.xFail();
	}

}