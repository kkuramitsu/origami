package blue.origami.nez.peg.expression;

import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.ExpressionVisitor;

public class PDispatch extends PArray {
	public final byte[] indexMap;

	public PDispatch(Expression[] inners, byte[] indexMap) {
		super(inners);
		this.indexMap = indexMap;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitDispatch(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<switch");
		for (int i = 0; i < this.size(); i++) {
			ByteSet bs = new ByteSet();
			for (int j = 0; j < this.indexMap.length; j++) {
				if ((this.indexMap[j] & 0xff) == i + 1) {
					bs.set(j, true);
				}
			}
			sb.append(" ");
			bs.strOut(sb);
			sb.append("->");
			this.get(i).strOut(sb);
		}
		sb.append(">");
	}

	public final static boolean isConsumed(Expression e) {
		if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny) {
			return true;
		}
		return false;
	}
}