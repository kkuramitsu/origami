package origami.nez2;

public class ParseTree implements T, OStrings {
	static final String EmptyTag = "";
	static final String EmptyLabel = EmptyTag;
	static final String ErrorTag = "err*";

	String tag;
	byte[] inputs;
	int spos;
	int epos;
	TreeLink child;

	ParseTree(String tag, byte[] inputs, int spos, int epos, T child) {
		this.inputs = inputs;
		this.spos = spos;
		this.epos = epos;
		this.child = (TreeLink) child;
		this.tag = (tag == EmptyTag && this.child != null) ? this.child.tag() : tag;
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[#");
		sb.append(this.tag);
		int c = sb.length();
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			cur.strOut(sb);
		}
		if (c == sb.length()) {
			sb.append(" '");
			sb.append(this.asString());
			sb.append("'");
		}
		sb.append("]");
	}

	public final boolean is(String tag) {
		return tag.equals(this.tag);
	}

	public final String tag() {
		return this.tag;
	}

	public final ParseTree get(String label) {
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			if (cur.tag != null && label.equals(cur.tag)) {
				return cur.child;
			}
		}
		return null;
	}

	public final ParseTree[] list() {
		int c = 0;
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			if (cur.child != null) {
				c++;
			}
		}
		ParseTree[] ts = new ParseTree[c];
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			if (cur.child != null) {
				ts[--c] = cur.child;
			}
		}
		return ts;
	}

	public final String asString() {
		return new String(this.inputs, this.spos, this.epos - this.spos);
	}

	public final Token asToken(String path) {
		return new Token(this.asString(), path, this.inputs, this.spos, this.epos);
	}

}

interface T {
}

class TreeLink implements T {
	String tag;
	ParseTree child;
	TreeLink prev;

	TreeLink(String tag, ParseTree child, TreeLink prev) {
		this.tag = tag;
		this.child = child;
		this.prev = prev;
	}

	void strOut(StringBuilder sb) {
		if (this.child != null) {
			if (this.tag != null) {
				sb.append(" $" + this.tag + "=");
			}
			this.child.strOut(sb);
		}
	}

	String tag() {
		for (TreeLink cur = this; cur != null; cur = cur.prev) {
			if (cur.child == null) {
				return cur.tag;
			}
		}
		return ParseTree.EmptyTag;
	}

}
