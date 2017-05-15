package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.Expression;

public abstract class PUnary extends Expression {
	protected Expression inner;

	protected PUnary(Expression inner) {
		this.inner = inner;
	}

	@Override
	public final int size() {
		return 1;
	}

	@Override
	public final Expression get(int index) {
		return this.inner;
	}

	@Override
	public final Expression set(int index, Expression e) {
		Expression old = this.inner;
		this.inner = e;
		return old;
	}

	protected final void formatUnary(String prefix, String suffix, StringBuilder sb) {
		if (prefix != null) {
			sb.append(prefix);
		}
		String pre = "(";
		String post = ")";
		if (this.inner instanceof PNonTerminal || this.inner instanceof PTerm || this.inner instanceof PTree
				|| Expression.isMultiBytes(this.inner)) {
			pre = "";
			post = "";
		}
		sb.append(pre);
		this.get(0).strOut(sb);
		sb.append(post);
		if (suffix != null) {
			sb.append(suffix);
		}
	}
}