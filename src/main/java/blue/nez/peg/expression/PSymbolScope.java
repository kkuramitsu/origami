package blue.nez.peg.expression;

import blue.nez.ast.Symbol;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;

public class PSymbolScope extends PFunction<Symbol> {
	public PSymbolScope(NezFunc op, Symbol table, Expression e) {
		super(op, table, e);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitSymbolScope(this, a);
	}
}