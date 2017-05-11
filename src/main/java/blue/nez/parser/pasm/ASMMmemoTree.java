package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.pasm.PegAsm.AbstMemo;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public final class ASMMmemoTree extends AbstMemo {
	public ASMMmemoTree(MemoPoint m, PAsmInst next) {
		super(m, m.isStateful(), next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTMemo(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xSuccPos();
		sc.setTreeMemo(this.uid, ppos);
		return this.next;
	}

}