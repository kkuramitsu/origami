package blue.origami.parser.peg;

import blue.origami.nez.ast.Symbol;

public class PSymbolScope extends PUnary {
	public final Symbol label;

	public PSymbolScope(Symbol label, Expression e) {
		super(e);
		this.label = label;
	}

	public PSymbolScope(Expression e) {
		this(null, e);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitSymbolScope(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<");
		if (this.label == null) {
			sb.append("block");
		} else {
			sb.append("local");
		}
		sb.append(" ");
		this.get(0).strOut(sb);
		sb.append(">");
	}

}