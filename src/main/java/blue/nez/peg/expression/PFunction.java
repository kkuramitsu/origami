package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.NezFunc;

public abstract class PFunction<T> extends PUnary {
	public final NezFunc funcName;
	public final T param;

	protected PFunction(NezFunc op, T param, Expression e, Object ref) {
		super(e == null ? defaultEmpty : e, ref);
		this.funcName = op;
		this.param = param;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PFunction<?>) {
			PFunction<?> f = (PFunction<?>) o;
			return this.funcName == f.funcName && this.param.equals(f.param) && this.get(0).equals(f.get(0));
		}
		return false;
	}

	public boolean hasInnerExpression() {
		return this.get(0) != defaultEmpty;
	}

	/* function */
	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<");
		sb.append(this.funcName);
		if (this.param != null) {
			sb.append(" ");
			sb.append(this.param);
		}
		if (this.hasInnerExpression()) {
			sb.append(" ");
			sb.append(this.get(0));
		}
		sb.append(">");
	}
}