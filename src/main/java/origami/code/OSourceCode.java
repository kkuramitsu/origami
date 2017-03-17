package origami.code;

import origami.nez.ast.Tree;
import origami.trait.Handled;
import origami.trait.OStringOut;
import origami.type.OType;

public abstract class OSourceCode<T> implements OCode, Handled<T>, OStringOut {
	private T handled;
	private OType rtype;

	public OSourceCode(T handled, OType ret) {
		this.handled = handled;
		this.rtype = ret;
	}

	@Override
	public T getHandled() {
		return handled;
	}

	protected void setHandled(T t) {
		this.handled = t;
	}

	@Override
	public OType getType() {
		return this.rtype;
	}

	protected void setType(OType t) {
		this.rtype = t;
	}

	private Tree<?> s = null;

	@Override
	public OCode setSource(Tree<?> s) {
		if (this.s == null) {
			this.s = s;
			for (OCode n : this.getParams()) {
				if (n == null) {
					continue;
				}
				n.setSource(s);
			}
		}
		return this;
	}

	@Override
	public Tree<?> getSource() {
		return this.s;
	}

	// String

	@Override
	public String toString() {
		return OStringOut.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		sb.append(this.getCodeName());
		sb.append("[");
		// strOutInner(sb);
		OStringOut.append(sb, this.getHandled());
		sb.append("]");
		for (OCode c : this.getParams()) {
			sb.append(" ");
			OStringOut.append(sb, c);
		}
		sb.append("):");
		sb.append(this.getType());
		if (this.getMatchCost() > 0) {
			sb.append("<");
			sb.append(this.getMatchCost());
			sb.append(">");
		}
	}

	protected String getCodeName() {
		return this.getClass().getSimpleName().replace("Code", "").toLowerCase();
	}

	protected void strOutInner(StringBuilder sb) {
		// sb.append(" ");
	}

}
