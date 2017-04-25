package blue.nez.parser.pegasm;

import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstMemo;

public final class ASMMmemoTree extends AbstMemo {
	public ASMMmemoTree(MemoPoint m, PegAsmInst next) {
		super(m, m.isStateful(), next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTMemo(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xSuccPos();
		sc.setTreeMemo(this.uid, ppos);
		return this.next;
	}

}