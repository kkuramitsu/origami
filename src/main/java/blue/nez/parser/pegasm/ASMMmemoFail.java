package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.parser.pegasm.PegAsm.AbstMemo;

public final class ASMMmemoFail extends AbstMemo {
	public ASMMmemoFail(MemoPoint m) {
		super(m, m.isStateful(), null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitMemoFail(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.setFailMemo(this.uid);
		return sc.raiseFail();
	}

}