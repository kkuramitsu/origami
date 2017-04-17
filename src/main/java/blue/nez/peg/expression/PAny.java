package blue.nez.peg.expression;

import blue.nez.peg.ExpressionVisitor;

/**
 * Expression.Any represents an any character, denoted as . in Expression.
 * 
 * @author kiki
 *
 */

public class PAny extends PTerm {

	public PAny() {
	}

	@Override
	public final boolean equals(Object o) {
		return (o instanceof PAny);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitAny(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(".");
	}

}