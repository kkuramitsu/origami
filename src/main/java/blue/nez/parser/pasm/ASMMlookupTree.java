package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.pasm.PegAsm.AbstMemo;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public final class ASMMlookupTree extends AbstMemo {
	public final Symbol label;

	public ASMMlookupTree(MemoPoint m, PAsmInst next, PAsmInst skip) {
		super(m, m.isStateful(), next, skip);
		this.label = null;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTLookup(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
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