package blue.origami.parser.peg;

public class PTrap extends PTerm {
	public int trapid;
	public int uid;

	public PTrap(int trapid, int uid) {
		this.trapid = trapid;
		this.uid = uid;
	}

	@Override
	protected Object[] extract() {
		return new Object[] { this.trapid, this.uid };
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