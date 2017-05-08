/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.nez.parser;

import java.util.HashMap;

import blue.nez.ast.Source;
import blue.nez.ast.Symbol;
import blue.origami.util.OStringUtils;

public abstract class ParserContext<T> {
	public int pos = 0;
	public T left;
	protected final TreeConstructor<T> newTree;
	protected final TreeConnector<T> linkTree;

	public ParserContext(String s, long pos, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		this(new StringSource(s), pos, newTree, linkTree);
		this.inputs = ((StringSource) this.source).inputs;
		this.length = this.inputs.length - 1;
	}

	protected ParserContext(Source s, long pos, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		this.source = s;
		this.inputs = null;
		this.length = 0;
		this.pos = (int) pos;
		this.left = null;
		this.newTree = newTree;
		this.linkTree = linkTree;
	}

	public abstract <K> ParserContext<K> newInstance(Source s, long pos, TreeConstructor<K> newTree,
			TreeConnector<K> linkTree);

	public abstract long getPosition();

	public abstract long getMaximumPosition();

	public abstract void start();

	public abstract void end();

	protected Source source;
	private byte[] inputs;
	private int length;

	public boolean eof() {
		return !(this.pos < this.length);
	}

	public int read() {
		return this.inputs[this.pos++] & 0xff;
	}

	public int prefetch() {
		return this.inputs[this.pos] & 0xff;
	}

	public final void move(int shift) {
		this.pos += shift;
	}

	public void backtrack(int pos) {
		this.pos = pos;
	}

	public boolean match(byte[] text) {
		int len = text.length;
		if (this.pos + len > this.length) {
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

	public byte[] subByte(int startIndex, int endIndex) {
		byte[] b = new byte[endIndex - startIndex];
		System.arraycopy(this.inputs, (startIndex), b, 0, b.length);
		return b;
	}

	protected byte byteAt(int n) {
		return this.inputs[n];
	}

	// Tree Construction

	private enum Operation {
		Link, Tag, Replace, New;
	}

	static class TreeLog {
		Operation op;
		int pos;
		Object value;
		Object tree;
	}

	private TreeLog[] logs = new TreeLog[0];
	private int unused_log = 0;

	private void log2(Operation op, int pos, Object value, T tree) {
		if (!(this.unused_log < this.logs.length)) {
			TreeLog[] newlogs = new TreeLog[this.logs.length + 1024];
			System.arraycopy(this.logs, 0, newlogs, 0, this.logs.length);
			for (int i = this.logs.length; i < newlogs.length; i++) {
				newlogs[i] = new TreeLog();
			}
			this.logs = newlogs;
		}
		TreeLog l = this.logs[this.unused_log];
		l.op = op;
		l.pos = pos;
		l.value = value;
		l.tree = tree;
		this.unused_log++;
	}

	public final void beginTree(int shift) {
		this.log2(Operation.New, this.pos + shift, null, null);
	}

	public final void linkTree(T parent, Symbol label) {
		this.log2(Operation.Link, 0, label, this.left);
	}

	public final void tagTree(Symbol tag) {
		this.log2(Operation.Tag, 0, tag, null);
	}

	public final void valueTree(String value) {
		this.log2(Operation.Replace, 0, value, null);
	}

	public final void foldTree(int shift, Symbol label) {
		this.log2(Operation.New, this.pos + shift, null, null);
		this.log2(Operation.Link, 0, label, this.left);
	}

	@SuppressWarnings("unchecked")
	public final void endTree(int shift, Symbol tag, String value) {
		int objectSize = 0;
		TreeLog start = null;
		int start_index = 0;
		for (int i = this.unused_log - 1; i >= 0; i--) {
			TreeLog l = this.logs[i];
			if (l.op == Operation.Link) {
				objectSize++;
				continue;
			}
			if (l.op == Operation.New) {
				start = l;
				start_index = i;
				break;
			}
			if (l.op == Operation.Tag && tag == null) {
				tag = (Symbol) l.value;
			}
			if (l.op == Operation.Replace && value == null) {
				value = (String) l.value;
			}
		}
		this.left = this.newTree(tag, start.pos, (this.pos + shift), objectSize, value);
		if (objectSize > 0) {
			int n = 0;
			for (int j = start_index; j < this.unused_log; j++) {
				TreeLog l = this.logs[j];
				if (l.op == Operation.Link) {
					this.linkTree.link(this.left, n++, (Symbol) l.value, (T) l.tree);
					l.tree = null;
				}
			}
		}
		this.storeTreeLog(start_index);
	}

	public final T newTree(Symbol tag, int start, int end, int n, String value) {
		if (tag == null) {
			tag = Symbol.Null;
		}
		return this.newTree.newTree(tag, this.source, start, (end - start), n, value);
	}

	public final int loadTreeLog() {
		return this.unused_log;
	}

	public final void storeTreeLog(int log) {
		if (this.unused_log > log) {
			this.unused_log = log;
		}
	}

	public final T loadTree() {
		return this.left;
	}

	public final void storeTree(T tree) {
		this.left = tree;
	}

	// Symbol Table ---------------------------------------------------------

	static class SymbolEntry {
		Object value;
		SymbolEntry prev;

		SymbolEntry(Object value, SymbolEntry prev) {
			this.value = value;
			this.prev = prev;
		}
	}

	public static class SymbolTable extends HashMap<Symbol, SymbolEntry> {
		private static final long serialVersionUID = 1L;

		private SymbolTable parent = null;

		public SymbolTable(Symbol label, Object value) {
			super();
			this.put(label, new SymbolEntry(value, null));
		}

		private SymbolTable(SymbolTable parent) {
			this.parent = parent;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			return false;
		}

		private SymbolEntry getLocal(Symbol label) {
			return this.get(label);
		}

		public SymbolEntry getEntry(Symbol label) {
			for (SymbolTable st = this; st != null; st = st.parent) {
				SymbolEntry e = st.getLocal(label);
				if (e != null) {
					if (st != this) {
						this.put(label, e);
					}
					return e;
				}
			}
			return null;
		}

		public SymbolTable addEntry(Symbol label, Object value) {
			SymbolTable st = new SymbolTable(this);
			SymbolEntry entry = this.getEntry(label);
			st.put(label, new SymbolEntry(value, entry));
			return st;
		}

		public SymbolTable removeEntry(Symbol label) {
			SymbolEntry entry = this.getEntry(label);
			if (entry == null) {
				return this;
			}
			SymbolTable st = new SymbolTable(this);
			st.put(label, null);
			return st;
		}

	}

	private SymbolTable symbolTable = null;

	public final SymbolTable loadSymbolTable() {
		return this.symbolTable;
	}

	public final void storeSymbolTable(Object ref) {
		this.symbolTable = (SymbolTable) ref;
	}

	public interface SymbolAction {
		public void mutate(ParserContext<?> px, Symbol label, int ppos, Object thunk);
	}

	public interface SymbolPredicate {
		public boolean match(ParserContext<?> px, Symbol label, int ppos, Object thunk);
	}

	public static class SymbolDefinition implements SymbolAction {
		@Override
		public void mutate(ParserContext<?> px, Symbol label, int ppos, Object thunk) {
			byte[] extracted = px.subByte(ppos, px.pos);
			if (px.symbolTable == null) {
				px.symbolTable = new SymbolTable(label, extracted);
			} else {
				px.symbolTable = px.symbolTable.addEntry(label, extracted);
			}
		}

		@Override
		public String toString() {
			return "symbol";
		}
	}

	public static class SymbolReset implements SymbolAction {
		@Override
		public void mutate(ParserContext<?> px, Symbol label, int ppos, Object thunk) {
			if (px.symbolTable != null) {
				px.symbolTable = px.symbolTable.removeEntry(label);
			}
		}

		@Override
		public String toString() {
			return "reset";
		}
	}

	public static class SymbolExist implements SymbolPredicate {
		@Override
		public boolean match(ParserContext<?> px, Symbol label, int ppos, Object option) {
			if (px.symbolTable == null || px.symbolTable.getEntry(label) == null) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "exists";
		}
	}

	public static class SymbolExistString implements SymbolPredicate {
		@Override
		public boolean match(ParserContext<?> px, Symbol label, int ppos, Object option) {
			if (px.symbolTable == null) {
				return false;
			}
			for (SymbolEntry entry = px.symbolTable.getEntry(label); entry != null; entry = entry.prev) {
				if (option.equals(entry.value)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "exists";
		}

	}

	public static class SymbolMatch implements SymbolPredicate {
		@Override
		public boolean match(ParserContext<?> px, Symbol label, int ppos, Object option) {
			if (px.symbolTable == null) {
				return false;
			}
			SymbolEntry entry = px.symbolTable.getEntry(label);
			if (entry == null) {
				return false;
			}
			return px.match((byte[]) entry.value);
		}

		@Override
		public String toString() {
			return "match";
		}

	}

	public static class SymbolEquals implements SymbolPredicate {
		@Override
		public boolean match(ParserContext<?> px, Symbol label, int ppos, Object option) {
			if (px.symbolTable == null) {
				return false;
			}
			SymbolEntry entry = px.symbolTable.getEntry(label);
			if (entry == null) {
				return false;
			}
			byte[] extracted = px.subByte(ppos, px.pos);
			return extracted.equals(entry.value);
		}

		@Override
		public String toString() {
			return "&equals";
		}
	}

	public static class SymbolContains implements SymbolPredicate {
		@Override
		public boolean match(ParserContext<?> px, Symbol label, int ppos, Object option) {
			if (px.symbolTable == null) {
				return false;
			}
			byte[] extracted = px.subByte(ppos, px.pos);
			for (SymbolEntry entry = px.symbolTable.getEntry(label); entry != null; entry = entry.prev) {
				if (extracted.equals(entry.value)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "&contains";
		}
	}

	// Counter ------------------------------------------------------------

	private int count = 0;

	public final void scanCount(int ppos, long mask, int shift) {
		if (mask == 0) {
			String num = OStringUtils.newString(this.subByte(ppos, this.pos));
			this.count = (int) Long.parseLong(num);
		} else {
			long v = 0;
			for (int i = ppos; i < this.pos; i++) {
				int n = this.byteAt(i) & 0xff;
				v <<= 8;
				v |= n;
			}
			v = v & mask;
			this.count = (int) ((v & mask) >> shift);
		}
		// factory.verbose("set count %d mask=%s, shift=%s", count, mask,
		// shift);
	}

	public final boolean decCount() {
		return this.count-- > 0;
	}

	// Memotable ------------------------------------------------------------

	public final static int NotFound = 0;
	public final static int SuccFound = 1;
	public final static int FailFound = 2;

	private static class MemoEntry<E> {
		long key = -1;
		public int consumed;
		public E memoTree;
		public int result;
		public SymbolTable state;
	}

	private MemoEntry<T>[] memoArray = null;
	private int shift = 0;

	@SuppressWarnings("unchecked")
	public final void initMemoTable(int w, int n) {
		this.memoArray = new MemoEntry[w * n + 1];
		for (int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntry<>();
			this.memoArray[i].key = -1;
			this.memoArray[i].result = NotFound;
		}
		this.shift = (int) (Math.log(n) / Math.log(2.0)) + 1;
	}

	final long longkey(long pos, int memoPoint, int shift) {
		return ((pos << shift) | memoPoint) & Long.MAX_VALUE;
	}

	public final int lookupMemo(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint, this.shift);
		int hash = (int) (key % this.memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		if (m.key == key /* && m.state == this.symbolTable */) {
			this.pos += m.consumed;
			return m.result;
		}
		return NotFound;
	}

	public final int lookupTreeMemo(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint, this.shift);
		int hash = (int) (key % this.memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		if (m.key == key /* && m.state == this.symbolTable */) {
			this.pos += m.consumed;
			this.left = m.memoTree;
			return m.result;
		}
		return NotFound;
	}

	public final boolean statMemo(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint, this.shift);
		int hash = (int) (key % this.memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		return (m.key == key);
	}

	public final void setSuccMemo(int memoPoint, int ppos) {
		long key = this.longkey(ppos, memoPoint, this.shift);
		int hash = (int) (key % this.memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = this.left;
		m.consumed = this.pos - ppos;
		m.result = SuccFound;
		m.state = this.symbolTable;
	}

	public final void setTreeMemo(int memoPoint, int ppos) {
		long key = this.longkey(ppos, memoPoint, this.shift);
		int hash = (int) (key % this.memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = this.left;
		m.consumed = this.pos - ppos;
		m.result = SuccFound;
		m.state = this.symbolTable;
	}

	public final void setFailMemo(int memoPoint) {
		long key = this.longkey(this.pos, memoPoint, this.shift);
		int hash = (int) (key % this.memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = this.left;
		m.consumed = 0;
		m.result = FailFound;
		m.state = this.symbolTable;
	}

}
