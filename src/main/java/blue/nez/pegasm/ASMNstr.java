package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.pegasm.PegAsm.AbstStr;

public final class ASMNstr extends AbstStr {

	public ASMNstr(byte[] byteSeq, PegAsmInstruction next) {
		super(byteSeq, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNStr(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		if (!sc.match(this.utf8)) {
			return this.next;
		}
		return sc.xFail();
	}

}