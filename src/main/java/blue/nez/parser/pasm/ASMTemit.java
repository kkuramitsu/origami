package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMTemit extends PAsmInst {
	public final Symbol label;

	public ASMTemit(Symbol label, PAsmInst next) {
		super(next);
		this.label = label;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitTEmit(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		return this.next;
	}

}