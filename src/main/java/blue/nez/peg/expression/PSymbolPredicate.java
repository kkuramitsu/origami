package blue.nez.peg.expression;

import blue.nez.ast.Symbol;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;

public class PSymbolPredicate extends PFunction<String> {
	public Symbol table;
	public String symbol;

	public PSymbolPredicate(NezFunc op, String param, PNonTerminal pat, Object ref) {
		super(op, param, pat, ref);
		int l = param.indexOf('+');
		if (l > 0) {
			this.symbol = param.substring(l + 1);
			param = param.substring(0, l);
		}
		this.table = Symbol.unique(param);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitSymbolPredicate(this, a);
	}
}