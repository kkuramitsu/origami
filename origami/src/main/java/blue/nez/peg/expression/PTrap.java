package blue.nez.peg.expression;

import blue.nez.peg.ExpressionVisitor;

public class PTrap extends PTerm {
	public int trapid;
	public int uid;

	public PTrap(int trapid, int uid) {
		this.trapid = trapid;
		this.uid = uid;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PTrap) {
			PTrap l = (PTrap) o;
			return this.trapid == l.trapid && this.uid == l.uid;
		}
		return false;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitTrap(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<trap " + this.trapid + " " + this.uid + ">");
	}
}