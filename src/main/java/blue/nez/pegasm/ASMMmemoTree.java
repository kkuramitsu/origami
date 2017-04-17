package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.pegasm.PegAsm.AbstMemo;

public final class ASMMmemoTree extends AbstMemo {
	public ASMMmemoTree(MemoPoint m, PegAsmInstruction next) {
		super(m, m.isStateful(), next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTMemo(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xSuccPos();
		sc.setTreeMemo(this.uid, ppos);
		return this.next;
	}

}