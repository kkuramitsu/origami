package origami.code;

import origami.nez.ast.SourcePosition;
import origami.type.OType;
import origami.util.Handled;
import origami.util.StringCombinator;

public abstract class OSourceCode<T> implements OCode, Handled<T>, StringCombinator {
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


	private SourcePosition s = SourcePosition.UnknownPosition;
	
	@Override
	public OCode setSourcePosition(SourcePosition s) {
		if (this.s == SourcePosition.UnknownPosition) {
			this.s = s;
			for (OCode n : this.getParams()) {
				n.setSourcePosition(s);
			}
		}
		return this;
	}

	@Override
	public SourcePosition getSourcePosition() {
		return this.s;
	}

	// String

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		sb.append(this.getCodeName());
		Object handled = this.getHandled();
		if(handled != null) {
			sb.append("[");
			StringCombinator.append(sb, handled);
			sb.append("]");
		}
		for (OCode c : this.getParams()) {
			sb.append(" ");
			SourcePosition cs = c.getSourcePosition();
			if(cs == this.getSourcePosition()) {
				StringCombinator.append(sb, c);
			}
			else {
				sb.append(this.getCodeName());
				sb.append(":");
				StringCombinator.append(sb, c.getType());
			}
		}
		sb.append("):");
		StringCombinator.append(sb, this.getType());
//		if (this.getMatchCost() > 0) {
//			sb.append("<");
//			sb.append(this.getMatchCost());
//			sb.append(">");
//		}
	}

	protected String getCodeName() {
		return this.getClass().getSimpleName().replace("Code", "").toLowerCase();
	}

//	protected void strOutInner(StringBuilder sb) {
//		// sb.append(" ");
//	}

}
