package blue.nez.peg.expression;

import blue.nez.ast.Symbol;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

public class PTree extends PUnary {
	public int beginShift = 0;
	public int endShift = 0;
	public final boolean folding;
	public final Symbol label;
	public Symbol tag = null; // optimization parameter
	public String value = null; // optimization parameter

	public PTree(boolean folding, Symbol label, int beginShift, Expression inner, Symbol tag, String value,
			int endShift) {
		super(inner);
		this.folding = folding;
		this.label = label;
		this.beginShift = beginShift;
		this.endShift = endShift;
		this.tag = tag;
		this.value = value;
	}

	public PTree(Expression inner) {
		this(false, null, 0, inner, null, null, 0);
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PTree) {
			PTree t = (PTree) o;
			if (t.beginShift != this.beginShift || t.endShift != this.endShift || t.tag != this.tag
					|| t.value != this.value) {
				return false;
			}
			return t.get(0).equals(this.get(0));
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitTree(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append('{');
		if (this.folding) {
			sb.append('$');
			if (this.label != null) {
				sb.append(this.label);
			}
			sb.append(" ");
		}
		this.get(0).strOut(sb);
		if (this.value != null) {
			sb.append(" ");
			formatReplace(this.value, sb);
		}
		if (this.tag != null) {
			sb.append(" #");
			sb.append(this.tag);
		}
		sb.append(" }");
	}
}