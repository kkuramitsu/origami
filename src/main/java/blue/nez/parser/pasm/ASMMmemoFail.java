package blue.nez.parser.pasm;

import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.pasm.PegAsm.AbstMemo;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public final class ASMMmemoFail extends AbstMemo {
	public ASMMmemoFail(MemoPoint m) {
		super(m, m.isStateful(), null);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitMemoFail(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		sc.setFailMemo(this.uid);
		return sc.raiseFail();
	}

}