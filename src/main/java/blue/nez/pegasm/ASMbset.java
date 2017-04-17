package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstSet;

public class ASMbset extends AbstSet {
	public ASMbset(boolean[] byteMap, PegAsmInstruction next) {
		super(byteMap, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSet(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int byteChar = sc.read();
		if (this.byteSet[byteChar]) {
			return this.next;
		}
		return sc.xFail();
	}

}