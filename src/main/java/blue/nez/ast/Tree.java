package blue.nez.ast;

import java.util.AbstractList;

import blue.nez.parser.ParserSource;
import blue.nez.parser.TreeConnector;
import blue.nez.parser.TreeConstructor;
import blue.origami.util.OStringUtils;
import blue.origami.util.StringCombinator;

public abstract class Tree<E extends Tree<E>> extends AbstractList<E>
		implements SourcePosition, StringCombinator, TreeConstructor<E>, TreeConnector<E> {
	protected final static Symbol[] EmptyLabels = new Symbol[0];

	protected Symbol tag;
	// protected Source source;
	protected int pos;
	protected int length;
	protected Symbol[] subTreeLabels;
	protected E[] subTree;
	protected Object value;

	protected Tree() {
		this.tag = Symbol.Null;
		// this.source = null;
		this.pos = 0;
		this.length = 0;
		this.subTree = null;
		this.value = null;
		this.subTreeLabels = EmptyLabels;
	}

	protected Tree(Symbol tag, Source source, long pos, int len, E[] subTree, Object value) {
		this.tag = tag;
		// this.source = source;
		this.pos = (int) pos;
		this.length = len;
		this.subTree = subTree;
		this.value = value == null ? source : value;
		this.subTreeLabels = (this.subTree != null) ? new Symbol[this.subTree.length] : EmptyLabels;
	}

	@Override
	public final void link(E parent, int n, Symbol label, E child) {
		parent.set(n, label, child);
	}

	protected abstract E dupImpl();

	public final E dup() {
		E t = this.dupImpl();
		if (this.subTree != null) {
			for (int i = 0; i < this.subTree.length; i++) {
				if (this.subTree[i] != null) {
					t.subTree[i] = this.subTree[i].dup();
					t.subTreeLabels[i] = this.subTreeLabels[i];
				}
			}
		}
		return t;
	}

	/* Source */

	@Override
	public final Source getSource() {
		if (this.value instanceof Source) {
			return (Source) this.value;
		}
		return null;
	}

	@Override
	public final long getSourcePosition() {
		return this.pos;
	}

	public final int getLength() {
		return this.length;
	}

	/* Tag, Type */

	public final Symbol getTag() {
		return this.tag;
	}

	public final boolean is(Symbol tag) {
		return tag == this.getTag();
	}

	@Override
	public int size() {
		return this.subTreeLabels.length;
	}

	@Override
	public final boolean isEmpty() {
		return this.size() == 0;
	}

	@Override
	public E get(int index) {
		return this.subTree[index];
	}

	public final Symbol getLabel(int index) {
		return this.subTreeLabels[index];
	}

	@Override
	public final E set(int index, E node) {
		E oldValue = null;
		oldValue = this.subTree[index];
		this.subTree[index] = node;
		return oldValue;
	}

	public final void set(int index, Symbol label, E node) {
		this.subTreeLabels[index] = label;
		this.subTree[index] = node;
	}

	public final int indexOf(Symbol label) {
		for (int i = 0; i < this.subTreeLabels.length; i++) {
			if (this.subTreeLabels[i] == label) {
				return i;
			}
		}
		return -1;
	}

	public final boolean has(Symbol label) {
		for (int i = 0; i < this.subTreeLabels.length; i++) {
			if (this.subTreeLabels[i] == label) {
				return true;
			}
		}
		return false;
	}

	public final E get(Symbol label) {
		for (int i = 0; i < this.subTreeLabels.length; i++) {
			if (this.subTreeLabels[i] == label) {
				return this.subTree[i];
			}
		}
		throw this.newNoSuchLabel(label);
	}

	protected RuntimeException newNoSuchLabel(Symbol label) {
		return new RuntimeException("undefined label: " + label);
	}

	public final E get(Symbol label, E defval) {
		for (int i = 0; i < this.subTreeLabels.length; i++) {
			if (this.subTreeLabels[i] == label) {
				return this.subTree[i];
			}
		}
		return defval;
	}

	public final int size(Symbol label, int size) {
		for (int i = 0; i < this.subTreeLabels.length; i++) {
			if (this.subTreeLabels[i] == label) {
				return this.subTree[i].size();
			}
		}
		return size;
	}

	/* Value */

	public final Object getValue() {
		return this.value;
	}

	public final void setValue(Object value) {
		this.value = value;
	}

	public final byte[] getBytes() {
		if (this.getSource() != null) {
			long pos = this.getSourcePosition();
			long epos = pos + this.length;
			return this.getSource().subBytes(pos, epos);
		}
		return new byte[0];
	}

	public final String getString() {
		Source s = this.getSource();
		if (s != null) {
			long pos = this.getSourcePosition();
			long epos = pos + this.length;
			byte[] chunks = s.subBytes(pos, epos);
			if (OStringUtils.isValidUTF8(chunks)) {
				return OStringUtils.newString(chunks);
			}
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatBytes(sb, chunks);
			return sb.toString();
		}
		if (this.value instanceof String) {
			return (String) this.value;
		}
		return "";
	}

	// subtree method

	public final boolean isAt(Symbol label, Symbol tag) {
		for (int i = 0; i < this.subTreeLabels.length; i++) {
			if (this.subTreeLabels[i] == label) {
				return this.subTree[i].is(tag);
			}
		}
		return false;
	}

	public final String getStringAt(int index, String defval) {
		if (index < this.size()) {
			return this.get(index).getString();
		}
		return defval;
	}

	public final String getStringAt(Symbol label, String defval) {
		for (int i = 0; i < this.subTreeLabels.length; i++) {
			if (this.subTreeLabels[i] == label) {
				return this.getStringAt(i, defval);
			}
		}
		return defval;
	}

	/**
	 * Create new input stream
	 * 
	 * @return SourceContext
	 */

	public final Source toSource() {
		return ParserSource.newStringSource(this.getSource().getResourceName(),
				this.getSource().linenum(this.getSourcePosition()), this.getString());
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[#");
		if (this.getTag() != null) {
			sb.append(this.getTag().getSymbol());
		}
		if (this.subTree == null) {
			sb.append(" ");
			byte[] chunks = this.getBytes();
			if (OStringUtils.isValidUTF8(chunks)) {
				OStringUtils.formatStringLiteral(sb, '\'', this.getString(), '\'');
			} else {
				OStringUtils.formatBytes(sb, chunks);
			}
		} else {
			for (int i = 0; i < this.size(); i++) {
				sb.append(" ");
				if (this.subTreeLabels[i] != null) {
					sb.append("$");
					sb.append(this.subTreeLabels[i].getSymbol());
					sb.append("=");
				}
				StringCombinator.append(sb, this.subTree[i]);
			}
		}
		sb.append("]");
	}
}
