package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.pasm.PegAsm.AbstStr;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMOstr extends AbstStr {
	public ASMOstr(byte[] byteSeq, PAsmInst next) {
		super(byteSeq, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitOStr(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		sc.match(this.utf8);
		return this.next;
	}

}