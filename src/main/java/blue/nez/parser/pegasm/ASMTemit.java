package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTemit extends PegAsmInst {
	public final Symbol label;

	public ASMTemit(Symbol label, PegAsmInst next) {
		super(next);
		this.label = label;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTEmit(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		return this.next;
	}

}