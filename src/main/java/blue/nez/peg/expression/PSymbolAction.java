package blue.nez.peg.expression;

import blue.nez.ast.Symbol;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;

public class PSymbolAction extends PFunction<String> {
	public final Symbol table;

	public PSymbolAction(NezFunc op, String param, PNonTerminal e, Object ref) {
		super(op, param, e, ref);
		this.table = Symbol.nullUnique(param);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitSymbolAction(this, a);
	}

}