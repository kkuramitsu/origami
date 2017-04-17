package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTtag extends PegAsmInstruction {
	public final Symbol tag;

	public ASMTtag(Symbol tag, PegAsmInstruction next) {
		super(next);
		this.tag = tag;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTTag(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		sc.tagTree(this.tag);
		return this.next;
	}

}