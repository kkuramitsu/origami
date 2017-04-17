package blue.nez.pegasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.pegasm.PegAsm.AbstractTableInstruction;

public final class ASMSdef2 extends AbstractTableInstruction {
	public final SymbolAction action;

	public ASMSdef2(SymbolAction action, Symbol label, PegAsmInstruction next) {
		super(label, next);
		this.action = action;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		//v.visitSDef(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		this.action.mutate(sc, this.label, sc.pos);
		return this.next;
	}

}