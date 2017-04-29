// Tree Construction Functions

public static interface NewFunc<T> {
	public T newTree(String tag, byte[] inputs, int pos, int len, int size, String value);
}

public static interface SetFunc<T> {
	public T setTree(T parent, int n, String label, T child);
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
	int pos;

	final boolean eof() {
		return !(this.pos < this.inputs.length - 1);
	}

	final int getbyte() {
		return this.inputs[this.pos] & 0xff;
	}

	final boolean move(int shift) {
		this.pos += shift;
		return true;
	}

	final int read() {
		return this.inputs[this.pos++] & 0xff;
	}

	final boolean matchBytes(byte[] text) {
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

	private NewFunc<T> newFunc = (tag, inputs, pos, len, size, value) -> null;
	private SetFunc<T> setFunc = (parent, n, label, child) -> null;

	T tree;
	TreeLog<T> treeLog = null;

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

	// SymbolTable

	Object state = null;

	// Backtrack
	
	public final boolean back(int pos) {
		this.pos = pos;
		return true;
	}

	public final boolean backT(T tree) {
		this.tree = tree;
		return true;
	}
	
	public final boolean backL(TreeLog<T> treeLog) {
		this.treeLog = treeLog;
		return true;
	}
	
	public final boolean backS(Object state) {
		this.state = state;
		return true;
	}
	
	// short cut 

	final boolean is(int ch) {
		return this.inputs[this.pos++] == ch;
	}
	
	final boolean back2(int pos, Object state) {
		this.pos = pos;
		this.state = state;
		return true;
	}

	final boolean back3(int pos, T tree, TreeLog<T> treeLog) {
		this.pos = pos;
		this.tree = tree;
		this.treeLog = treeLog;
		return true;
	}

	final boolean back4(int pos, T tree, TreeLog<T> treeLog, Object state) {
		this.pos = pos;
		this.tree = tree;
		this.treeLog = treeLog;
		this.state = state;
		return true;
	}

	// Memotable

	private static class MemoEntry<T> {
		long key = -1;
		int consumed;
		T memoTree;
		int result;
		Object state;
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
		if (m.key == key && (!STATEFUL || m.state == this.state)) {
			this.pos += m.consumed;
			return m.result;
		}
		return NotFound;
	}

	public final int memoLookupTree(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		if (m.key == key && (!STATEFUL || m.state == this.state)) {
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
		m.consumed = this.pos - ppos;
		m.state = this.state;
		m.result = SuccFound;
		return true;
	}

	public final boolean memoSuccTree(int memoPoint, int ppos) {
		long key = this.longkey(ppos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.memoTree = this.tree;
		m.consumed = this.pos - ppos;
		m.state = this.state;
		m.result = SuccFound;
		return true;
	}

	public final boolean memoFail(int memoPoint, int ppos) {
		long key = this.longkey(ppos, memoPoint);
		MemoEntry<T> m = this.getMemo(key);
		m.key = key;
		m.consumed = 0;
		m.state = this.state;
		m.result = FailFound;
		return false;
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

static byte[] t(String s) {
	int len = s.length();
	for (int i = 0; i < s.length(); i++) {
		char c = s.charAt(i);
		if(c == '~') len -= 2;
	}	
	byte[] b = new byte [len];
	int p = 0;
	for (int i = 0; i < s.length(); i++) {
		char c = s.charAt(i);
		if(c != '~') {
			b[p] = (byte)c;
		}
		else {
			String hex2 = s.substring(i+1, i+3);
			b[p] = (byte)Integer.parseInt(hex2, 16);
			i+=2;
		}
		p++;
	}
	//System.out.println("DEBUG " + s + " => " + new String(b));
	return b;
}

