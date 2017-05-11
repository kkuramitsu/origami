package blue.nez.peg.expression;

import java.util.ArrayList;
import java.util.List;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.Pair is a pair representation of Expression.Sequence.
 * 
 * @author kiki
 *
 */

/* OK (a (b c)) */
/* NG ((a b) c) */

public class PPair extends PBinary {
	public PPair(Expression first, Expression next) {
		super(first, next);
		assert (!(first instanceof PPair));
	}

	@Override
	public Expression desugar() {
		if (this.left instanceof PEmpty) {
			return this.right;
		}
		if (this.right instanceof PEmpty) {
			return this.left;
		}
		return this;
	}

	public void expand(List<Expression> l) {
		Expression.addSequence(l, this.left);
		if (this.right instanceof PPair) {
			((PPair) this.right).expand(l);
		} else {
			Expression.addSequence(l, this.right);
		}
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitPair(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.left instanceof PByte) {
			List<Integer> l = new ArrayList<>();
			Expression lasting = extractMultiBytes(this, l);
			if (l.size() > 1) {
				sb.append("'");
				for (int c : l) {
					ByteSet.formatByte(c, "'", "\\x%02x", sb);
				}
				sb.append("'");
				if (!(lasting instanceof PEmpty)) {
					sb.append(" ");
					lasting.strOut(sb);
				}
				return;
			}
		}
		this.formatPair(" ", sb);
	}
}

abstract class PBinary extends Expression {
	public Expression left;
	public Expression right;

	protected PBinary(Expression left, Expression right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public final int size() {
		return 2;
	}

	@Override
	public final Expression get(int index) {
		assert (index < 2);
		return index == 0 ? this.left : this.right;
	}

	@Override
	public final Expression set(int index, Expression e) {
		assert (index < 2);
		if (index == 0) {
			Expression p = this.left;
			this.left = e;
			return p;
		} else {
			Expression p = this.right;
			this.right = e;
			return p;
		}
	}

	public void formatPair(String delim, StringBuilder sb) {
		if (this.left instanceof PChoice) {
			sb.append("(");
			this.left.strOut(sb);
			sb.append(")");
		} else {
			this.left.strOut(sb);
		}
		sb.append(delim);
		if (this.right instanceof PChoice) {
			sb.append("(");
			this.right.strOut(sb);
			sb.append(")");
		} else {
			this.right.strOut(sb);
		}
	}

}