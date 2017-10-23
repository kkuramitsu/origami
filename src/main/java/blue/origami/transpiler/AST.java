package blue.origami.transpiler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;

import blue.origami.common.OArrays;
import blue.origami.common.OSource;
import blue.origami.common.OStringUtils;
import blue.origami.common.OStrings;
import blue.origami.common.SourcePosition;
import blue.origami.common.Symbol;
import blue.origami.parser.ParserSource;
import blue.origami.parser.pasm.PAsmAPI.TreeFunc;
import blue.origami.parser.pasm.PAsmAPI.TreeSetFunc;

public abstract class AST implements SourcePosition, OStrings, Iterable<AST>, TreeFunc, TreeSetFunc {
	protected Symbol tag;
	protected OSource s;
	protected int pos;

	AST(Symbol tag, OSource s, int pos) {
		this.tag = tag;
		this.s = s;
		this.pos = pos;
	}

	@Override
	public AST apply(Symbol tag, OSource s, int spos, int epos, int nsubs, Object value) {
		if (nsubs == 0) {
			return new ASTString(tag, s, spos, s.subString(spos, epos));
		}
		if (value != null) {
			return new ASTString(tag, s, spos, value.toString());
		}
		return new ASTNode(tag, s, epos, epos - spos, nsubs);
	}

	@Override
	public AST apply(Object parent, int index, Symbol label, Object child) {
		ASTNode node = (ASTNode) parent;
		node.labels[index] = label;
		node.subTree[index] = (AST) child;
		return node;
	}

	private static HashMap<String, AST> nameMap = new HashMap<>();

	public final static AST getName(String name) {
		AST t = nameMap.get(name);
		if (t == null) {
			t = new ASTString(Symbol.Null, null, 0, name);
			nameMap.put(name, t);
		}
		return t;
	}

	public final static AST TreeFunc = getName("");

	public final static AST[] getNames(String... names) {
		return Arrays.stream(names).map(n -> getName(n)).toArray(AST[]::new);
	}

	public final static String[] names(AST[] names) {
		return Arrays.stream(names).map(n -> n.getString()).toArray(String[]::new);
	}

	// @Override
	// public void strOut(StringBuilder sb) {
	// // TODO Auto-generated method stub
	//
	// }

	@Override
	public final OSource getSource() {
		return this.s;
	}

	@Override
	public final long getSourcePosition() {
		return this.pos;
	}

	public final Symbol getTag() {
		return this.tag;
	}

	public final boolean is(String tag) {
		return tag.equals(this.getTag().toString());
	}

	public abstract int size();

	public abstract AST[] sub();

	@Override
	public final Iterator<AST> iterator() {
		class IterN implements Iterator<AST> {
			int loc;
			AST[] args;

			IterN(AST... args) {
				this.args = args;
				this.loc = 0;
			}

			@Override
			public boolean hasNext() {
				return this.loc < this.args.length;
			}

			@Override
			public AST next() {
				return this.args[this.loc++];
			}
		}
		return new IterN(this.sub());
	}

	public <T> T[] subMap(Function<AST, T> mapper, IntFunction<T[]> generator) {
		return Arrays.stream(this.sub()).map(mapper).toArray(generator);
	}

	public abstract AST get(int index);

	// public final Symbol getLabel(int index) {
	// return this.subTreeLabels[index];
	// }
	//
	// @Override
	// public final E set(int index, E node) {
	// E oldValue = null;
	// oldValue = this.subTree[index];
	// this.subTree[index] = node;
	// return oldValue;
	// }
	////
	//// public final void set(int index, Symbol label, E node) {
	//// this.subTreeLabels[index] = label;
	//// this.subTree[index] = node;
	//// }
	//
	// public final int indexOf(Symbol label) {
	// for (int i = 0; i < this.subTreeLabels.length; i++) {
	// if (this.subTreeLabels[i] == label) {
	// return i;
	// }
	// }
	// return -1;
	// }

	public abstract boolean has(Symbol label);

	public abstract AST get(Symbol label);

	// protected RuntimeException newNoSuchLabel(Symbol label) {
	// return new RuntimeException("undefined label: " + label);
	// }
	//
	// public int size(Symbol label, int size) {
	// return size;
	// }

	/* Value */

	public abstract String getString();

	// subtree method

	public final String getStringAt(int index, String defval) {
		if (index < this.size()) {
			return this.get(index).getString();
		}
		return defval;
	}

	public final String getStringAt(Symbol label, String defval) {
		AST sub = this.get(label);
		if (sub != null) {
			return sub.getString();
		}
		return defval;
	}

	/**
	 * Create new input stream
	 * 
	 * @return SourceContext
	 */

	public final OSource toSource() {
		return ParserSource.newStringSource(this.getSource().getResourceName(),
				this.getSource().linenum(this.getSourcePosition()), this.getString());
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

}

class ASTString extends AST {
	protected String token;

	ASTString(Symbol tag, OSource s, int pos, String token) {
		super(tag, s, pos);
		this.token = token;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[#");
		if (this.getTag() != null) {
			sb.append(this.getTag().getSymbol());
		}
		sb.append(" ");
		OStringUtils.formatStringLiteral(sb, '\'', this.token, '\'');
		sb.append("]");
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public AST[] sub() {
		return OArrays.emptyTrees;
	}

	@Override
	public AST get(int index) {
		return null;
	}

	@Override
	public boolean has(Symbol label) {
		return false;
	}

	@Override
	public AST get(Symbol label) {
		return null;
	}

	@Override
	public String getString() {
		return this.token;
	}

}

class ASTNode extends AST {

	protected int length;
	protected Symbol[] labels;
	protected AST[] subTree;

	// protected ASTNode() {
	// this.tag = Symbol.Null;
	// // this.source = null;
	// this.pos = 0;
	// this.length = 0;
	// this.subTree = null;
	// this.value = null;
	// this.labels = EmptyLabels;
	// }

	protected ASTNode(Symbol tag, OSource source, long pos, int len, int nsize) {
		super(tag, source, (int) pos);
		this.length = len;
		this.subTree = new AST[nsize];
		this.labels = new Symbol[nsize];
		assert (nsize > 0);
	}

	public final int getLength() {
		return this.length;
	}

	/* Tag, Type */

	// public final boolean is(Symbol tag) {
	// return tag == this.getTag();
	// }

	@Override
	public int size() {
		return this.labels.length;
	}

	@Override
	public AST[] sub() {
		return this.subTree;
	}

	// @Override
	// public final boolean isEmpty() {
	// return this.size() == 0;
	// }

	@Override
	public AST get(int index) {
		return this.subTree[index];
	}

	public final Symbol getLabel(int index) {
		return this.labels[index];
	}

	@Override
	public final boolean has(Symbol label) {
		for (int i = 0; i < this.labels.length; i++) {
			if (this.labels[i] == label) {
				return true;
			}
		}
		return false;
	}

	@Override
	public final AST get(Symbol label) {
		for (int i = 0; i < this.labels.length; i++) {
			if (this.labels[i] == label) {
				return this.subTree[i];
			}
		}
		return null;
	}

	// @Override
	// public final int size(Symbol label, int size) {
	// for (int i = 0; i < this.labels.length; i++) {
	// if (this.labels[i] == label) {
	// return this.subTree[i].size();
	// }
	// }
	// return size;
	// }

	/* Value */

	@Override
	public final String getString() {
		OSource s = this.getSource();
		if (s != null) {
			long pos = this.getSourcePosition();
			long epos = pos + this.length;
			return s.subString(pos, epos);
		}
		return "";
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[#");
		if (this.getTag() != null) {
			sb.append(this.getTag().getSymbol());
		}
		for (int i = 0; i < this.size(); i++) {
			sb.append(" ");
			if (this.labels[i] != null) {
				sb.append("$");
				sb.append(this.labels[i].getSymbol());
				sb.append("=");
			}
			OStrings.append(sb, this.subTree[i]);
		}
		sb.append("]");
	}

}
