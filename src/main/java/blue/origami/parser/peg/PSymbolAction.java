package blue.origami.parser.peg;

import blue.origami.common.Symbol;
import blue.origami.parser.pasm.PAsmAPI.SymbolFunc;

public class PSymbolAction extends PUnary {
	public final SymbolFunc action;
	public final Symbol label;
	public final Object thunk;

	public PSymbolAction(SymbolFunc func, Symbol label, PNonTerminal e) {
		super(e);
		this.action = func;
		this.label = label;
		this.thunk = null;
	}

	public PSymbolAction(SymbolFunc func, String label, PNonTerminal e) {
		this(func, Symbol.nullUnique(label), e);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitSymbolAction(this, a);
	}

	@Override
	public boolean isEmpty() {
		return this.get(0) instanceof PEmpty;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<");
		sb.append(this.action);
		sb.append(" ");
		sb.append(this.label);
		if (!this.isEmpty()) {
			sb.append(" ");
			sb.append(this.get(0));
		}
		sb.append(">");
	}

}