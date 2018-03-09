package blue.origami.peg;

class TreeNode implements T {
	static final String EmptyTag = "";

	String tag;
	byte[] inputs;
	int spos;
	int epos;
	TreeLink child;

	TreeNode(String tag, byte[] inputs, int spos, int epos, T child) {
		this.inputs = inputs;
		this.spos = spos;
		this.epos = epos;
		this.child = (TreeLink) child;
		this.tag = (tag == EmptyTag && this.child != null) ? this.child.tag() : tag;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.strOut(sb);
		return sb.toString();
	}

	void strOut(StringBuilder sb) {
		sb.append("[#");
		sb.append(this.tag);
		int c = sb.length();
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			cur.strOut(sb);
		}
		if (c == sb.length()) {
			sb.append(" '");
			sb.append(new String(this.inputs, this.spos, this.epos - this.spos));
			sb.append("'");
		}
		sb.append("]");
	}
}

interface T {
}

class TreeLink implements T {
	String tag;
	TreeNode child;
	TreeLink prev;

	TreeLink(String tag, T child, T prev) {
		this.tag = tag;
		this.child = (TreeNode) child;
		this.prev = (TreeLink) prev;
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
		return TreeNode.EmptyTag;
	}

}
