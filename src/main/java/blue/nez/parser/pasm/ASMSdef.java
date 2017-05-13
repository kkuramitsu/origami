package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class ASMSdef extends PAsmInst {
	public final Symbol tag;
	public final SymbolFunc action;

	public ASMSdef(SymbolFunc action, Symbol label, PAsmInst next) {
		super(next);
		this.tag = label;
		this.action = action;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		this.action.apply(px, px.state, this.tag, popPos(px));
		return this.next;
	}

}