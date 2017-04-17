package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserContext;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.pegasm.PegAsm.AbstMemo;

public final class ASMMlookupTree extends AbstMemo {
	public final Symbol label;

	public ASMMlookupTree(MemoPoint m, PegAsmInstruction next, PegAsmInstruction skip) {
		super(m, m.isStateful(), next, skip);
		this.label = null;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTLookup(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		switch (sc.lookupTreeMemo(this.uid)) {
		case ParserContext.NotFound:
			return this.next;
		case ParserContext.SuccFound:
			return this.jump;
		default:
			return sc.xFail();
		}
	}

}