package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstMemo;

public final class ASMMlookupTree extends AbstMemo {
	public final Symbol label;

	public ASMMlookupTree(MemoPoint m, PegAsmInst next, PegAsmInst skip) {
		super(m, m.isStateful(), next, skip);
		this.label = null;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTLookup(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		switch (sc.lookupTreeMemo(this.uid)) {
		case ParserContext.NotFound:
			return this.next;
		case ParserContext.SuccFound:
			return this.jump;
		default:
			return sc.raiseFail();
		}
	}

}