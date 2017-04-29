package blue.nez.peg.expression;

import blue.nez.ast.Symbol;
import blue.nez.peg.ExpressionVisitor;

public class PTag extends PTerm {
	public final Symbol tag;

	public final Symbol symbol() {
		return this.tag;
	}

	public PTag(Symbol tag) {
		this.tag = tag;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PTag) {
			return this.tag == ((PTag) o).tag;
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitTag(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("#" + this.tag);
	}
}