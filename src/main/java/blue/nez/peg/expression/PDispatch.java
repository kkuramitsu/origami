package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;

public class PDispatch extends PArray {
	public final byte[] indexMap;

	public PDispatch(Expression[] inners, byte[] indexMap, Object ref) {
		super(inners, ref);
		this.indexMap = indexMap;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PDispatch) {
			PDispatch d = (PDispatch) o;
			if (d.indexMap.length != this.indexMap.length) {
				return false;
			}
			for (int i = 0; i < this.indexMap.length; i++) {
				if (this.indexMap[i] != d.indexMap[i]) {
					return false;
				}
			}
			return this.equalsList((PArray) o);
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitDispatch(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<switch");
		for (int i = 0; i < this.size(); i++) {
			PByteSet bs = new PByteSet(null);
			for (int j = 0; j < this.indexMap.length; j++) {
				if ((this.indexMap[j] & 0xff) == i + 1) {
					bs.set(j, true);
				}
			}
			sb.append(" ");
			bs.strOut(sb);
			sb.append(":");
			this.get(i).strOut(sb);
		}
		sb.append(">");
	}
}