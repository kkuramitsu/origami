package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.pegasm.PegAsm.AbstractTableInstruction;

public final class ASMSpred2 extends AbstractTableInstruction {
	public final SymbolPredicate pred;
	public final Object option;

	public ASMSpred2(SymbolPredicate pred, Symbol label, Object option, PegAsmInstruction next) {
		super(label, next);
		this.pred = pred;
		this.option = option;
	}

	public ASMSpred2(SymbolPredicate pred, Symbol label, PegAsmInstruction next) {
		this(pred, label, null, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSIsDef(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return this.pred.match(sc, this.label, sc.pos, this.option) ? this.next : sc.xFail();
	}
}