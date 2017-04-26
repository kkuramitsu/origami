package blue.origami.nezcc;

import java.nio.charset.Charset;
import java.util.Scanner;

class NezParserContext<T> {

	public NezParserContext(String inputs, int memo, NewFunc<T> newFunc, SetFunc<T> setFunc) {
		this.inputs = inputs.getBytes(Charset.forName("UTF-8"));
		this.pos = 0;
		this.newFunc = newFunc;
		this.setFunc = setFunc;
		this.initMemo(64, memo);
	}

	byte[] inputs;
	public int pos;

	public boolean eof() {
		return !(this.pos < this.inputs.length);
	}

	public int getbyte() {
		return this.inputs[this.pos] & 0xff;
	}

	public final void move(int shift) {
		this.pos += shift;
	}

	public int read() {
		return this.inputs[this.pos++] & 0xff;
	}

	public boolean matchBytes(byte[] text) {
		int len = text.length;
		if (this.pos + len > this.inputs.length) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (text[i] != this.inputs[this.pos + i]) {
				return false;
			}
		}
		this.pos += len;
		return true;
	}

	// Tree Construction

	public T tree;

	private static enum TreeOp {
		Link, Tag, Value, New;
	}

	static class TreeLog<T> {
		TreeOp op;
		int pos;
		String symbol;
		T tree;
		TreeLog<T> prev;

		TreeLog(TreeOp op, int pos, String symbol, T tree, TreeLog<T> prev) {
			this.op = op;
			this.pos = pos;
			this.symbol = symbol;
			this.tree = tree;
			this.prev = prev;
		}
	}

	public TreeLog<T> treeLog = null;

	private void log(TreeOp op, int pos, String symbol, T tree) {
		this.treeLog = new TreeLog<>(op, pos, symbol, tree, this.treeLog);
	}

	public final boolean beginTree(int shift) {
		this.log(TreeOp.New, this.pos + shift, null, null);
		return true;
	}

	public final boolean linkTree(String label) {
		this.log(TreeOp.Link, 0, label, this.tree);
		return true;
	}

	public final boolean tagTree(String tag) {
		this.log(TreeOp.Tag, 0, tag, null);
		return true;
	}

	public final boolean valueTree(String value) {
		this.log(TreeOp.Value, 0, value, null);
		return true;
	}

	public final boolean foldTree(int shift, String label) {
		this.log(TreeOp.New, this.pos + shift, null, null);
		this.log(TreeOp.Link, 0, label, this.tree);
		return true;
	}

	public final boolean endTree(int shift, String tag, String value) {
		int ppos = 0;
		int objectSize = 0;
		TreeLog<T> sub = null;
		TreeLog<T> next = null;
		for (TreeLog<T> l = this.treeLog; l != null; l = next) {
			next = l.prev;
			if (l.op == TreeOp.Link) {
				objectSize++;
				l.prev = sub;
				sub = l;
				continue;
			}
			if (l.op == TreeOp.New) {
				this.treeLog = l.prev;
				ppos = l.pos;
				break;
			}
			if (l.op == TreeOp.Tag && tag == null) {
				tag = l.symbol;
			}
			if (l.op == TreeOp.Value && value == null) {
				value = l.symbol;
			}
		}
		this.tree = this.newFunc.newTree(tag, this.inputs, ppos, (this.pos + shift) - ppos, objectSize, value);
		if (objectSize > 0) {
			int n = 0;
			for (TreeLog<T> l = sub; l != null; l = l.prev) {
				this.tree = this.setFunc.setTree(this.tree, n++, l.symbol, l.tree);
			}
		}
		return true;
	}

	// Tree Construction Functions

	public static interface NewFunc<T> {
		public T newTree(String tag, byte[] inputs, int pos, int len, int size, String value);
	}

	public static interface SetFunc<T> {
		public T setTree(T parent, int n, String label, T child);
	}

	private NewFunc<T> newFunc = (tag, inputs, pos, len, size, value) -> null;
	private SetFunc<T> setFunc = (parent, n, label, child) -> null;

	// SymbolTable
	// ---------------------------------------------------------

	static class SymbolTable {
		String label;
		String value;
		SymbolTable prev;

		SymbolTable(String label, String value, SymbolTable prev) {
			this.label = label;
			this.value = value;
			this.prev = prev;
		}
	}

	public SymbolTable state = null;

	public void addSymbolEntry(String label, String value) {
		this.state = new SymbolTable(label, value, this.state);
	}

	public SymbolTable getSymbolEntry(String label) {
		for (SymbolTable st = this.state; st != null; st = st.prev) {
			if (label.equals(st.value)) {
				return st;
			}
		}
		return null;
	}

	public interface SymbolAction {
		public void mutate(NezParserContext<?> px, String label, int ppos);
	}

	public interface SymbolPredicate {
		public boolean match(NezParserContext<?> px, String label, int ppos, Object option);
	}

	public final boolean back(int pos) {
		this.pos = pos;
		return true;
	}

	public final boolean back(T tree) {
		this.tree = tree;
		return true;
	}

	public final boolean back(TreeLog<T> treeLog) {
		this.treeLog = treeLog;
		return true;
	}

	public final boolean back(SymbolTable state) {
		this.state = state;
		return true;
	}

	// Counter ------------------------------------------------------------

	private int count = 0;

	public final void scanCount(int ppos, long mask, int shift) {
		if (mask == 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = ppos; i < this.pos; i++) {
				sb.append((char) this.inputs[i]);
			}
			this.count = (int) Long.parseLong(sb.toString());
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = ppos; i < this.pos; i++) {
				sb.append(Integer.toBinaryString(this.inputs[i] & 0xff));
			}
			long v = Long.parseUnsignedLong(sb.toString(), 2);
			this.count = (int) ((v & mask) >> shift);
		}
		// Verbose.println("set count %d", count);
	}

	public final boolean decCount() {
		return this.count-- > 0;
	}

	// Memotable

	private static class MemoEntry<T> {
		long key = -1;
		int consumed;
		T memoTree;
		int result;
		SymbolTable state;
	}

	private MemoEntry<T>[] memoArray = null;

	public final static int NotFound = 0;
	public final static int SuccFound = 1;
	public final static int FailFound = 2;

	@SuppressWarnings("unchecked")
	public void initMemo(int w, int n) {
		this.memoArray = new MemoEntry[w * n + 1];
		for (int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntry<>();
			this.memoArray[i].key = -1;
			this.memoArray[i].result = NotFound;
		}
	}

	final long longkey(long pos, int memoPoint) {
		return ((pos << 12) | memoPoint);
	}

	final MemoEntry<T> getMemo(long key) {
		int hash = (int) (key % this.memoArray.length);
		return this.memoArray[hash];
	}

	public final int memoLookup(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		if (m.key == key) {
			this.pos += m.consumed;
			return m.result;
		}
		return NotFound;
	}

	public final int memoLookupTree(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		if (m.key == key) {
			this.pos += m.consumed;
			this.tree = m.memoTree;
			return m.result;
		}
		return NotFound;
	}

	public final boolean memoSucc(int memoPoint, int ppos) {
		long key = this.longkey(ppos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.memoTree = this.tree;
		m.consumed = this.pos - ppos;
		m.result = SuccFound;
		// m.stateValue = -1;
		return true;
	}

	public final boolean memoSuccTree(int memoPoint, int ppos) {
		long key = this.longkey(ppos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.memoTree = this.tree;
		m.consumed = this.pos - ppos;
		m.result = SuccFound;
		return true;
	}

	public final boolean memoFail(int memoPoint, int ppos) {
		long key = this.longkey(ppos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.consumed = 0;
		m.result = FailFound;
		return false;
	}

	// Utils

	static boolean[] boolMap(String s) {
		boolean[] b = new boolean[256];
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == 'T' || s.charAt(i) == '1') {
				b[i] = true;
			}
		}
		return b;
	}

	static int[] indexMap(String s) {
		int[] b = new int[256];
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > '0' && c <= '9') {
				b[i] = c - '0';
			} else if (c >= 'A' || c <= 'Z') {
				b[i] = (c - 'A') + 10;
			}
		}
		return b;
	}

	// Tree Construction

	public static class KeyValueTree {
		public String key;
		public Object value;

		KeyValueTree(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			this.strOut(sb);
			return sb.toString();
		}

		private void strOut(StringBuilder sb) {
			sb.append("[#");
			sb.append(this.key == null ? "" : this.key);
			if (this.value instanceof KeyValueTree[]) {
				KeyValueTree[] sub = (KeyValueTree[]) this.value;
				for (KeyValueTree child : sub) {
					sb.append(" ");
					if (child.key != null) {
						sb.append("$" + child.key + "=");
					}
					((KeyValueTree) child.value).strOut(sb);
				}
			} else {
				sb.append(" '");
				sb.append(this.value);
				sb.append("'");
			}
			sb.append("]");
		}
	}

	public static KeyValueTree newTree1(String tag, byte[] inputs, int pos, int len, int size, String value) {
		if (size == 0) {
			return new KeyValueTree(tag, value != null ? value : new String(inputs, pos, len));
		}
		return new KeyValueTree(tag, new KeyValueTree[size]);
	}

	public static KeyValueTree setTree1(KeyValueTree parent, int n, String label, KeyValueTree child) {
		KeyValueTree[] childs = (KeyValueTree[]) parent.value;
		childs[n] = new KeyValueTree(label, child);
		return parent;
	}

	static String readInputs(String[] a) {
		StringBuilder sb = new StringBuilder();
		if (a.length > 0) {
			sb.append(a[0]);
		} else {
			Scanner console = new Scanner(System.in);
			String s = console.nextLine();
			while (s != null) {
				sb.append(s);
				s = console.nextLine();
			}
			console.close();
		}
		return sb.toString();
	}

	static NezParserContext<KeyValueTree> getSampleParserContext(String[] a, int memo) {
		String inputs = readInputs(a);
		NewFunc<KeyValueTree> f = NezParserContext::newTree1;
		SetFunc<KeyValueTree> f2 = NezParserContext::setTree1;
		return new NezParserContext<>(inputs, memo, f, f2);
	}

	// Combinator

	public static interface ParserFunc<T> {
		boolean match(NezParserContext<T> px);
	}

	static final <T> boolean pOption(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		if (!f.match(px)) {
			px.pos = pos;
		}
		return true;
	}

	static final <T> boolean pOptionT(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		T tree = px.tree;
		TreeLog<T> treeLog = px.treeLog;
		if (!f.match(px)) {
			px.pos = pos;
			px.tree = tree;
			px.treeLog = treeLog;
		}
		return true;
	}

	static final <T> boolean pOptionTS(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		T tree = px.tree;
		TreeLog<T> treeLog = px.treeLog;
		SymbolTable state = px.state;
		if (!f.match(px)) {
			px.pos = pos;
			px.tree = tree;
			px.treeLog = treeLog;
			px.state = state;
		}
		return true;
	}

	static final <T> boolean pMany(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		while (f.match(px) && pos < px.pos) {
			pos = px.pos;
		}
		px.pos = pos;
		return true;
	}

	static final <T> boolean pManyT(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		T tree = px.tree;
		TreeLog<T> treeLog = px.treeLog;
		while (f.match(px) && pos < px.pos) {
			pos = px.pos;
			tree = px.tree;
			treeLog = px.treeLog;
		}
		px.pos = pos;
		px.tree = tree;
		px.treeLog = treeLog;
		return true;
	}

	static final <T> boolean pManyTS(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		T tree = px.tree;
		TreeLog<T> treeLog = px.treeLog;
		SymbolTable state = px.state;
		while (f.match(px) && pos < px.pos) {
			pos = px.pos;
			tree = px.tree;
			treeLog = px.treeLog;
			state = px.state;
		}
		px.pos = pos;
		px.tree = tree;
		px.treeLog = treeLog;
		px.state = state;
		return true;
	}

	static final <T> boolean pAnd(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		if (f.match(px)) {
			px.pos = pos;
			return true;
		}
		return false;
	}

	static final <T> boolean pNot(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		if (f.match(px)) {
			return false;
		}
		px.pos = pos;
		return true;
	}

	static final <T> boolean pNotT(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		T tree = px.tree;
		TreeLog<T> treeLog = px.treeLog;
		if (f.match(px)) {
			return false;
		}
		px.pos = pos;
		px.tree = tree;
		px.treeLog = treeLog;
		return true;
	}

	static final <T> boolean pNotTS(NezParserContext<T> px, ParserFunc<T> f) {
		int pos = px.pos;
		T tree = px.tree;
		TreeLog<T> treeLog = px.treeLog;
		SymbolTable state = px.state;
		if (f.match(px)) {
			return false;
		}
		px.pos = pos;
		px.tree = tree;
		px.treeLog = treeLog;
		px.state = state;
		return true;
	}

	static final <T> boolean pLink(NezParserContext<T> px, ParserFunc<T> f, String label) {
		T tree = px.tree;
		TreeLog<T> treeLog = px.treeLog;
		if (!f.match(px)) {
			return false;
		}
		px.linkTree(label);
		px.tree = tree;
		px.treeLog = treeLog;
		return true;
	}

	static final <T> boolean pMemo(NezParserContext<T> px, ParserFunc<T> f, int mp) {
		int pos = px.pos;
		switch (px.memoLookup(mp)) {
		case 0:
			return (f.match(px) && px.memoSucc(mp, pos)) || (px.memoFail(mp, pos));
		case 1:
			return true;
		default:
			return false;
		}
	}

	static final <T> boolean pMemoT(NezParserContext<T> px, ParserFunc<T> f, int mp) {
		int pos = px.pos;
		switch (px.memoLookupTree(mp)) {
		case 0:
			return (f.match(px) && px.memoSuccTree(mp, pos)) || (px.memoFail(mp, pos));
		case 1:
			return true;
		default:
			return false;
		}
	}

}
