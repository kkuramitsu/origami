package blue.origami.parser.peg;

import blue.origami.common.Symbol;

public class PTag extends PTerm {
	public final Symbol tag;

	public final Symbol symbol() {
		return this.tag;
	}

	public PTag(Symbol tag) {
		this.tag = tag;
	}

	@Override
	protected Object[] extract() {
		return new Object[] { this.tag, };
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