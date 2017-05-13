package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class ASMSpred2 extends PAsmInst {
	public final Symbol tag;
	public final SymbolFunc pred;

	public ASMSpred2(SymbolFunc pred, Symbol tag, PAsmInst next) {
		super(next);
		this.tag = tag;
		this.pred = pred;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.pred.apply(px, px.state, this.tag, px.pos) ? this.next : raiseFail(px);
	}
}