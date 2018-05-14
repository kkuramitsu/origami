package origami.tcode;

import origami.nez2.OStrings;
import origami.nez2.Token;

public class TCode implements OStrings {
	Token s;
	String name;
	Object value;
	TType ty;

	public TCode(Token s, String name, Object value, TType ty) {
		this.s = s;
		this.name = name;
		this.value = value;
		this.ty = ty;
	}

	public TCode(Token s, String name, TCode... subs) {
		this(s, name, subs, null);
	}

	public Token getSource() {
		if (this.s != null) {
			return this.s;
		}
		for (TCode sub : this.subs()) {
			Token s = sub.getSource();
			if (s != null) {
				return s;
			}
		}
		return null;
	}

	public String getName() {
		return this.name;
	}

	public @SuppressWarnings("unchecked") <T> T getValue() {
		return (T) this.value;
	}

	final static TCode[] empty = new TCode[0];

	public int size() {
		return this.subs().length;
	}

	public TCode[] subs() {
		return (this.value instanceof TCode[]) ? (TCode[]) this.value : empty;
	}

	public TType getType() {
		return this.ty;
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		sb.append(this.getName());
		if (this.size() == 0) {
			sb.append(" ");
			OStrings.append(sb, this.getValue());
		} else {
			for (TCode sub : this.subs()) {
				sb.append(" ");
				sub.strOut(sb);
			}
		}
		if (this.ty != null) {
			sb.append(" ::");
			OStrings.append(sb, this.ty);
		}
		sb.append(")");
	}
}
