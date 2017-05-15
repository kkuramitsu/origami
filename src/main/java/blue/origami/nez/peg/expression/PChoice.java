package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.ExpressionVisitor;

/**
 * Expression.Choice represents an ordered choice e / ... / e_n in Expression.
 * 
 * @author kiki
 *
 */

public class PChoice extends PArray {
	private boolean isUnordered;

	public PChoice(boolean isUnordered, Expression[] inners) {
		super(inners);
		this.isUnordered = isUnordered;
	}

	public final boolean isUnordered() {
		return this.isUnordered;
	}

	@Override
	public Object[] extract() {
		return (this.isUnordered) ? new Object[] { true } : super.extract();
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitChoice(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatList(this.isUnordered ? " | " : " / ", sb);
	}
}

abstract class PArray extends Expression {
	public Expression[] inners;

	PArray(Expression[] inners) {
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

	final void formatList(String delim, StringBuilder sb) {
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