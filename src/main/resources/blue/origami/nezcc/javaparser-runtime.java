// Tree Construction Functions

public static interface NewFunc<T> {
	public T newTree(String tag, byte[] inputs, int pos, int len, int size, String value);
}

public static interface SetFunc<T> {
	public T setTree(T parent, int n, String label, T child);
}

public static interface ParserFunc<T> {
	boolean match(NezParserContext<T> px);
}

static class NezParserContext<T> {

	NezParserContext(String inputs, int memo, NewFunc<T> newFunc, SetFunc<T> setFunc) {
		byte[] buf = inputs.getBytes(Charset.forName("UTF-8"));
		this.inputs = new byte[buf.length+1];
		System.arraycopy(buf, 0, this.inputs, 0, buf.length);
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

	public boolean match(byte[] text) {
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
		//System.out.println("treeLog: " + this.treeLog);
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

	private NewFunc<T> newFunc = (tag, inputs, pos, len, size, value) -> null;
	private SetFunc<T> setFunc = (parent, n, label, child) -> null;

	// SymbolTable
	// ---------------------------------------------------------

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

	// Backtrack
	
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

	public void memoSucc(int memoPoint, int ppos) {
		long key = this.longkey(this.pos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.memoTree = this.tree;
		m.consumed = this.pos - ppos;
		m.result = SuccFound;
		// m.stateValue = -1;
	}

	public void memoTreeSucc(int memoPoint, int ppos) {
		long key = this.longkey(this.pos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.memoTree = this.tree;
		m.consumed = this.pos - ppos;
		m.result = SuccFound;
	}

	public void memoFail(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.memoTree = this.tree;
		m.consumed = 0;
		m.result = FailFound;
	}

}

static enum TreeOp {
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
		} else if (c >= 'A' && c <= 'Z') {
			b[i] = (c - 'A') + 10;
		}
	}
	return b;
}

// Combinator

static final <T> boolean fail(NezParserContext<T> px) {
	return false;
}

static final <T> boolean pLink(NezParserContext<T> px, ParserFunc<T> f, String label) {
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	if (f.match(px)) {
		return false;
	}
	px.linkTree(label);
	px.tree = tree;
	px.treeLog = treeLog;
	return true;
}

