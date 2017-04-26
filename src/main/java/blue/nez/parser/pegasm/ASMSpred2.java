package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.parser.pegasm.PegAsm.AbstractTableInstruction;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public final class ASMSpred2 extends AbstractTableInstruction {
	public final SymbolPredicate pred;
	public final Object thunk;

	public ASMSpred2(SymbolPredicate pred, Symbol label, Object option, PegAsmInst next) {
		super(label, next);
		this.pred = pred;
		this.thunk = option;
	}

	public ASMSpred2(SymbolPredicate pred, Symbol label, PegAsmInst next) {
		this(pred, label, null, next);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSIsDef(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return this.pred.match(sc, this.label, sc.pos, this.thunk) ? this.next : sc.raiseFail();
	}
}