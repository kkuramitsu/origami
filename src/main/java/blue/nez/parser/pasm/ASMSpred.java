package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class ASMSpred extends PAsmInst {
	public final Symbol tag;
	public final SymbolFunc pred;

	public ASMSpred(SymbolFunc pred, Symbol tag, PAsmInst next) {
		super(next);
		this.tag = tag;
		this.pred = pred;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.pred.apply(px, px.state, this.tag, popPos(px)) ? this.next : raiseFail(px);
	}
}