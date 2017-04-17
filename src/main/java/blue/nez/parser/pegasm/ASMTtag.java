package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTtag extends PegAsmInst {
	public final Symbol tag;

	public ASMTtag(Symbol tag, PegAsmInst next) {
		super(next);
		this.tag = tag;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTTag(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.tagTree(this.tag);
		return this.next;
	}

}