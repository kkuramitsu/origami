package blue.nez.peg.expression;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

public class PSymbolPredicate extends PUnary {
	public final SymbolPredicate pred;
	public final Symbol label;
	public final Object thunk;

	public PSymbolPredicate(SymbolPredicate pred, Symbol label, PNonTerminal pat, Object thunk) {
		super(pat == null ? Expression.defaultEmpty : pat);
		this.pred = pred;
		this.label = label;
		this.thunk = thunk;
	}

	public PSymbolPredicate(SymbolPredicate pred, String label, PNonTerminal pat, Object thunk) {
		this(pred, Symbol.unique(label), pat, thunk);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitSymbolPredicate(this, a);
	}

	@Override
	public boolean isEmpty() {
		return this.get(0) instanceof PEmpty;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<");
		sb.append(this.pred);
		sb.append(" ");
		sb.append(this.label);
		if (!this.isEmpty()) {
			sb.append(" ");
			this.get(0).strOut(sb);
		}
		if (this.thunk != null) {
			sb.append(" ");
			sb.append(this.thunk);
		}
		sb.append(">");
	}

}