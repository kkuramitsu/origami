package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;
import blue.nez.parser.pegasm.PegAsm.AbstractTableInstruction;

public final class ASMSdef extends AbstractTableInstruction {
	public final SymbolAction action;
	public final Object thunk;

	public ASMSdef(SymbolAction action, Symbol label, Object thunk, PegAsmInst next) {
		super(label, next);
		this.action = action;
		this.thunk = thunk;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSDef(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		this.action.mutate(sc, this.label, sc.xPPos(), this.thunk);
		return this.next;
	}

}