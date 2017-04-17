package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.pegasm.PegAsm.AbstMemo;

public final class ASMMmemoSucc extends AbstMemo {
	public ASMMmemoSucc(MemoPoint m, PegAsmInstruction next) {
		super(m, m.isStateful(), next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitMemo(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xSuccPos();
		sc.setSuccMemo(this.uid, ppos);
		return this.next;
	}
}