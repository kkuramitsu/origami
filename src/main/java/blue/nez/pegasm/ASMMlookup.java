package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.pegasm.PegAsm.AbstMemo;

public final class ASMMlookup extends AbstMemo {
	public ASMMlookup(MemoPoint m, PegAsmInstruction next, PegAsmInstruction skip) {
		super(m, m.isStateful(), next, skip);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitLookup(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		switch (sc.lookupMemo(this.uid)) {
		case ParserContext.NotFound:
			return this.next;
		case ParserContext.SuccFound:
			return this.jump;
		default:
			return sc.xFail();
		}
	}
}