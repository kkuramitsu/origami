package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class Sdefe extends PAsmInst {
	public final Symbol tag;
	public final SymbolFunc action;

	public Sdefe(SymbolFunc action, Symbol tag, PAsmInst next) {
		super(next);
		this.tag = tag;
		this.action = action;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		this.action.apply(px, px.state, this.tag, px.pos);
		return this.next;
	}

}