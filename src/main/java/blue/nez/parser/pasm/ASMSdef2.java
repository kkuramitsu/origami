package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.pasm.PegAsm.AbstractTableInstruction;
import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public final class ASMSdef2 extends AbstractTableInstruction {
	public final SymbolAction action;
	public final Object thunk;

	public ASMSdef2(SymbolAction action, Symbol label, Object thunk, PAsmInst next) {
		super(label, next);
		this.action = action;
		this.thunk = thunk;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		// v.visitSDef(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		this.action.mutate(sc, this.label, sc.pos, this.thunk);
		return this.next;
	}

}