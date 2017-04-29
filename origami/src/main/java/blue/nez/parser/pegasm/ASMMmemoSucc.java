package blue.nez.parser.pegasm;

import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstMemo;

public final class ASMMmemoSucc extends AbstMemo {
	public ASMMmemoSucc(MemoPoint m, PegAsmInst next) {
		super(m, m.isStateful(), next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitMemo(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xSuccPos();
		sc.setSuccMemo(this.uid, ppos);
		return this.next;
	}
}