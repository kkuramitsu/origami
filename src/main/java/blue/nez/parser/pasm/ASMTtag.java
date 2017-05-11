package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTtag extends PAsmInst {
	public final Symbol tag;

	public ASMTtag(Symbol tag, PAsmInst next) {
		super(next);
		this.tag = tag;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTTag(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		sc.tagTree(this.tag);
		return this.next;
	}

}