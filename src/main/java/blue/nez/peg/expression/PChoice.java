package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.Choice represents an ordered choice e / ... / e_n in Expression.
 * 
 * @author kiki
 *
 */

public class PChoice extends PArray {

	public PChoice(Expression[] inners, Object ref) {
		super(inners, ref);
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PChoice) {
			return this.equalsList((PArray) o);
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitChoice(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatList(" / ", sb);
	}
}

abstract class PArray extends Expression {
	public Expression[] inners;

	protected PArray(Expression[] inners, Object ref) {
		super(ref);
		this.inners = inners;
	}

	@Override
	public final int size() {
		return this.inners.length;
	}

	@Override
	public final Expression get(int index) {
		return this.inners[index];
	}

	@Override
	public Expression set(int index, Expression e) {
		Expression oldExpresion = this.inners[index];
		this.inners[index] = e;
		return oldExpresion;
	}

	protected final boolean equalsList(PArray l) {
		if (this.size() == l.size()) {
			for (int i = 0; i < this.size(); i++) {
				if (!this.get(i).equals(l.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	protected final void formatList(String delim, StringBuilder sb) {
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sb.append(delim);
			}
			Expression inner = this.get(i);
			if (inner instanceof PChoice) {
				sb.append("(");
				inner.strOut(sb);
				sb.append(")");
			} else {
				inner.strOut(sb);
			}
		}
	}
}