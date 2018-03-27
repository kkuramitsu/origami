package nez2;

import blue.origami.common.OStrings;

public class TreeNode implements T, OStrings {
	static final String EmptyTag = "";
	static final String EmptyLabel = EmptyTag;

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
			sb.append(new String(this.inputs, this.spos, this.epos - this.spos));
			sb.append("'");
		}
		sb.append("]");
	}

	public final boolean is(String tag) {
		return tag.equals(this.tag);
	}

	public final String gettag() {
		return this.tag;
	}

	public final TreeNode get(String label) {
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			if (cur.tag != null && label.equals(cur.tag)) {
				return cur.child;
			}
		}
		return null;
	}

	public final TreeNode[] list(String label) {
		int c = 0;
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			if (cur.child != null) {
				c++;
			}
		}
		TreeNode[] ts = new TreeNode[c];
		for (TreeLink cur = this.child; cur != null; cur = cur.prev) {
			if (cur.child != null) {
				ts[--c] = cur.child;
			}
		}
		return ts;
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
