package blue.nez.parser.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.pegasm.PegAsm.AbstractTableInstruction;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public final class ASMSdef extends AbstractTableInstruction {
	public final SymbolAction action;

	public ASMSdef(SymbolAction action, Symbol label, PegAsmInst next) {
		super(label, next);
		this.action = action;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitSDef(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		this.action.mutate(sc, this.label, sc.xPPos());
		return this.next;
	}

}