package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstStr;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMRstr extends AbstStr {
	public ASMRstr(byte[] byteSeq, PegAsmInst next) {
		super(byteSeq, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRStr(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		while (sc.match(this.utf8)) {
		}
		return this.next;
	}

}