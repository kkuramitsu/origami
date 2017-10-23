package blue.origami.parser.peg;

import blue.origami.common.Symbol;

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
	protected Object[] extract() {
		if (this.folding) {
			return new Object[] { this.label, this.beginShift, this.tag, this.value, this.endShift };
		}
		return new Object[] { this.beginShift, this.tag, this.value, this.endShift };
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
			sb.append(new PValue(this.value));
		}
		if (this.tag != null) {
			sb.append(" #");
			sb.append(this.tag);
		}
		sb.append(" }");
	}
}