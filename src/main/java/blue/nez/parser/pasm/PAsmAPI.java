package blue.nez.parser.pasm;

import blue.nez.ast.Source;
import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext;
import blue.nez.parser.TrapAction;
import blue.origami.util.OStringUtils;

public class PAsmAPI {

	public static class NezParserContext implements ParserContext {
		Source s;
		int pos;
		Object tree;
		TreeLog treeLog;
		TreeFunc newFunc;
		TreeSetFunc setFunc;
		State state;
		MemoEntry[] memos;
		TreeLog uLog;
		State uState;

		NezParserContext(Source s, int pos, TreeFunc newFunc, TreeSetFunc setFunc) {
			this.s = s;
			this.pos = pos;
			this.tree = null;
			this.treeLog = null;
			this.newFunc = newFunc;
			this.setFunc = setFunc;
			this.state = null;
			this.memos = null;
			this.uLog = null;
			this.uState = null;
		}

		void backtrack(int pos) {
			this.pos = pos;
		}

	}

	public static class TreeLog {
		int op;
		int pos;
		Object data;
		Object tree;
		TreeLog prevLog;

		TreeLog() {
			this.op = 0;
			this.pos = 0;
			this.data = null;
			this.tree = null;
			this.prevLog = null;
		}
	}

	public static class State {
		Symbol tag;
		int cnt;
		byte[] value;
		State prevState;

		State() {
			this.tag = null;
			this.cnt = 0;
			this.value = null;
			this.prevState = null;
		}

		State(Symbol tag, int cnt, byte[] value, State prev) {
			this.tag = tag;
			this.cnt = cnt;
			this.value = value;
			this.prevState = prev;
		}
	}

	public interface TreeFunc {
		Object apply(Symbol tag, Source s, int spos, int epos, int nsubs, Object value);
	}

	public interface TreeSetFunc {
		Object apply(Object parent, int index, Symbol label, Object child);
	}

	public static final int getbyte(NezParserContext px) {
		return px.s.byteAt(px.pos);
	}

	public static final int nextbyte(NezParserContext px) {
		return px.s.byteAt(px.pos++);
	}

	public static final boolean move(NezParserContext px, int shift) {
		px.pos = px.pos + shift;
		return true;
	}

	public static final boolean match(NezParserContext px, byte ch) {
		return nextbyte(px) == ch;
	}

	public static final boolean neof(NezParserContext px) {
		return !px.s.eof(px.pos);
	}

	public static final boolean matchBytes(NezParserContext px, byte[] utf8) {
		if (px.s.match(px.pos, utf8)) {
			return move(px, utf8.length);
		}
		return false;
	}

	public static final boolean ftrue(NezParserContext px) {
		return true;
	}

	public static final boolean ffalse(NezParserContext px) {
		return false;
	}

	/* Tree Construction */

	public static final TreeLog useTreeLog(NezParserContext px) {
		if (px.uLog == null) {
			return new TreeLog();
		}
		TreeLog uLog = px.uLog;
		px.uLog = uLog.prevLog;
		return uLog;
	}

	public static final TreeLog unuseTreeLog(NezParserContext px, TreeLog treeLog) {
		TreeLog uLog = px.treeLog;
		while (uLog != treeLog) {
			TreeLog prevLog = uLog;
			uLog = uLog.prevLog;
			prevLog.prevLog = px.uLog;
			px.uLog = prevLog;
		}
		return treeLog;
	}

	public static final boolean logTree(NezParserContext px, int op, int pos, Object data, Object tree) {
		TreeLog treeLog = useTreeLog(px);
		treeLog.op = op;
		treeLog.pos = pos;
		treeLog.data = data;
		treeLog.tree = tree;
		treeLog.prevLog = px.treeLog;
		px.treeLog = treeLog;
		return true;
	}

	public static final boolean beginTree(NezParserContext px, int shift) {
		return logTree(px, 0, px.pos + shift, null, null);
	}

	public static final boolean tagTree(NezParserContext px, Symbol tag) {
		return logTree(px, 1, 0, tag, null);
	}

	public static final boolean valueTree(NezParserContext px, Object value) {
		return logTree(px, 2, 0, value, null);
	}

	public static final boolean linkTree(NezParserContext px, Symbol label) {
		return logTree(px, 3, 0, label, px.tree);
	}

	public static final boolean foldTree(NezParserContext px, int shift, Symbol label) {
		return beginTree(px, shift) && linkTree(px, label);
	}

	public static final boolean endTree(NezParserContext px, int shift, Symbol tag, Object value) {
		int cnt = 0;
		TreeLog treeLog = px.treeLog;
		TreeLog prevLog = px.treeLog;
		while (prevLog.op != 0) {
			if (prevLog.op == 3) {
				cnt = cnt + 1;
			}
			if (tag == null && prevLog.op == 1) {
				tag = (Symbol) prevLog.data;
			}
			if (value == null && prevLog.op == 2) {
				value = prevLog.data;
			}
			prevLog = prevLog.prevLog;
		}
		px.tree = px.newFunc.apply(tag, px.s, prevLog.pos, px.pos + shift, cnt, value);
		prevLog = treeLog;
		while (prevLog.op != 0) {
			if (prevLog.op == 3) {
				cnt = cnt - 1;
				px.tree = px.setFunc.apply(px.tree, cnt, (Symbol) prevLog.data, prevLog.tree);
			}
			prevLog = prevLog.prevLog;
		}
		px.treeLog = unuseTreeLog(px, prevLog.prevLog);
		return true;
	}

	public static final boolean backLink(NezParserContext px, TreeLog treeLog, Symbol label, Object tree) {
		px.treeLog = unuseTreeLog(px, treeLog);
		linkTree(px, label);
		px.tree = tree;
		return true;
	}

	// public static final boolean initMemo(NezParserContext px) {
	// return true;
	// }
	//
	// public static final State createState(Symbol tag, int cnt, byte[] value,
	// State prevState) {
	// State state = new State();
	// state.tag = tag;
	// state.cnt = cnt;
	// state.value = value;
	// state.prevState = prevState;
	// return state;
	// }

	public interface SymbolFunc {
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos);
	}

	public static byte[] extract(NezParserContext px, int ppos, int pos) {
		return px.s.subBytes(ppos, pos);
	}

	public static class SymbolDefFunc implements SymbolFunc {
		@Override
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos) {
			px.state = new State(tag, 0, extract(px, ppos, px.pos), state);
			return true;
		}

		@Override
		public String toString() {
			return "symbol";
		}

	}

	public static class SymbolResetFunc implements SymbolFunc {
		@Override
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos) {

			return true;
		}

		@Override
		public String toString() {
			return "reset";
		}
	}

	public static class SymbolExistFunc implements SymbolFunc {
		@Override
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos) {
			if (state != null) {
				return (state.tag == tag) ? true : this.apply(px, state.prevState, tag, ppos);
			}
			return false;
		}

		@Override
		public String toString() {
			return "exists";
		}

	}

	public static class SymbolExistString implements SymbolFunc {
		final byte[] thunk;

		public SymbolExistString(String symbol) {
			this.thunk = OStringUtils.utf8(symbol);
		}

		@Override
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos) {
			if (state != null) {
				return (state.tag == tag && this.thunk.equals(state.value)) ? true
						: this.apply(px, state.prevState, tag, ppos);
			}
			return false;
		}

		@Override
		public String toString() {
			return "exists";
		}
	}

	public static class SymbolMatchFunc implements SymbolFunc {
		@Override
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos) {
			if (state != null) {
				return (state.tag == tag) ? matchBytes(px, state.value) : this.apply(px, state.prevState, tag, ppos);
			}
			return false;
		}

		@Override
		public String toString() {
			return "match";
		}
	}

	public static class SymbolEqualsFunc implements SymbolFunc {
		@Override
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos) {
			if (state != null) {
				return (state.tag == tag) ? state.equals(extract(px, ppos, px.pos))
						: this.apply(px, state.prevState, tag, ppos);
			}
			return false;
		}

		@Override
		public String toString() {
			return "&equals";
		}
	}

	public static class SymbolContainsFunc implements SymbolFunc {
		@Override
		public boolean apply(NezParserContext px, State state, Symbol tag, int ppos) {
			if (state != null) {
				return (state.tag == tag && state.equals(extract(px, ppos, px.pos))) ? true
						: this.apply(px, state.prevState, tag, ppos);
			}
			return false;
		}

		@Override
		public String toString() {
			return "&contains";
		}
	}

	// public static final boolean symbol1(NezParserContext px, State state,
	// Symbol tag, int pos) {
	// byte[] value = extract(px, pos);
	// int length = px.pos - pos;
	// px.state = createState(tag, length, value, px.state);
	// return true;
	// }
	//
	// public static final boolean exist1(NezParserContext px, State state,
	// Symbol tag, int pos) {
	// return state == null ? false : state.tag == tag ? true : exist1(px,
	// state.prevState, tag, pos);
	// }

	/* Memoization */

	public final static int NotFound = 0;
	public final static int SuccFound = 1;
	public final static int FailFound = 2;

	static class MemoEntry {
		long key;
		int result;
		int pos;
		Object data;

		MemoEntry(long key) {
			this.key = key;
			this.result = 0;
			this.pos = 0;
			this.data = null;
		}
	}

	static void initMemo(NezParserContext px, int w, int m) {
		int cnt = 0, max = w * m + 1;
		px.memos = new MemoEntry[max];
		while (cnt < max) {
			px.memos[cnt] = new MemoEntry(-1);
			cnt = cnt + 1;
		}
	}

	static final long longkey(long key, int memoPoint) {
		return (key << 12) + memoPoint;
	}

	public static final MemoEntry getMemo(NezParserContext px, long key) {
		return px.memos[(int) key % px.memos.length];
	}

	public static final int lookupMemo1(NezParserContext px, int memoPoint) {
		long key = longkey(px.pos, memoPoint);
		MemoEntry m = getMemo(px, key);
		if (m.key == key) {
			px.pos = m.pos;
			return m.result;
		}
		return NotFound;
	}

	public static final int lookupMemo3(NezParserContext px, int memoPoint) {
		long key = longkey(px.pos, memoPoint);
		MemoEntry m = getMemo(px, key);
		if (m.key == key) {
			px.pos = m.pos;
			px.tree = m.data;
			return m.result;
		}
		return NotFound;
	}

	public static final boolean storeMemo(NezParserContext px, int memoPoint, int pos, boolean matched) {
		long key = longkey(pos, memoPoint);
		MemoEntry m = getMemo(px, key);
		m.key = key;
		if (matched) {
			m.result = SuccFound;
			m.pos = px.pos;
		} else {
			m.result = FailFound;
			m.pos = pos;
		}
		m.data = px.tree;
		return matched;
	}

	// Virtual Machine

	public static class PAsmContext extends NezParserContext {

		public PAsmContext(Source s, int pos, TreeFunc newFunc, TreeSetFunc setFunc) {
			super(s, pos, newFunc, setFunc);
			this.head_pos = pos;
			initVM(this);
		}

		private int head_pos;

		@Override
		void backtrack(int pos) {
			if (pos > this.head_pos) {
				this.head_pos = pos;
			}
			this.pos = pos;
		}

		public long getMaximumPosition() {
			return this.head_pos;
		}

		PAsmStack unused;
		private PAsmStack longjmp;

		TrapAction[] actions;

		public final void setTrap(TrapAction[] array) {
			this.actions = array;
		}

		public final void trap(int trapid, int uid) {
			this.actions[trapid].performed(this, uid);
		}

	}

	public static class PAsmStack {
		int pos;
		Object tree;
		TreeLog treeLog;
		State state;
		PAsmInst jump;
		PAsmStack longjmp;
		final PAsmStack prev;
		PAsmStack next;

		PAsmStack(PAsmStack prev) {
			this.prev = prev;
			this.next = null;
		}
	}

	private static void push(PAsmContext px, PAsmStack used) {
		assert (px.unused == used);
		if (used.next == null) {
			used.next = new PAsmStack(used);
		}
		px.unused = used.next;
	}

	private static final PAsmStack pop(PAsmContext px) {
		px.unused = px.unused.prev;
		return px.unused;
	}

	public static final void pushFail(PAsmContext px, PAsmInst jump) {
		PAsmStack s = px.unused;
		s.pos = px.pos;
		s.tree = px.tree;
		s.treeLog = px.treeLog;
		s.state = px.state;
		s.jump = jump;
		s.longjmp = px.longjmp;
		px.longjmp = s;
		push(px, s);
	}

	public static final int popFail(PAsmContext px) { // used in succ
		PAsmStack s = px.longjmp;
		px.longjmp = s.longjmp;
		px.unused = s;
		return s.pos;
	}

	public static final PAsmInst raiseFail(PAsmContext px)/* popFail() */ {
		PAsmStack s = px.longjmp;
		// System.out.printf("fail ppos=%d, pos=%d jump=%s\n", s.pos, px.pos,
		// s.jump);
		px.backtrack(s.pos);
		px.tree = s.tree;
		px.treeLog = s.treeLog;
		px.state = s.state;
		// PAsmInst jump = s.jump;
		px.longjmp = s.longjmp;
		px.unused = s;
		return s.jump;
	}

	public static final PAsmInst updateFail(PAsmContext px, PAsmInst next) {
		PAsmStack s = px.longjmp;
		assert (s.longjmp != null); // FIXME
		// System.out.printf("ppos=%d, pos=%d\n", s.pos, px.pos);
		if (s.pos == px.pos) {
			return raiseFail(px);
		}
		s.pos = px.pos;
		s.tree = px.tree;
		s.treeLog = px.treeLog;
		s.state = px.state;
		return next;
	}

	public static void pushRet(PAsmContext px, PAsmInst jump) {
		PAsmStack s = px.unused;
		s.pos = -1; // FIXME
		s.longjmp = null; // FIXME
		s.jump = jump;
		push(px, s);
	}

	public static final PAsmInst popRet(PAsmContext px) {
		PAsmStack s = pop(px);
		if (s.pos != -1) {
			System.out.println("s.pos = " + s.pos);
		}
		assert (s.pos == -1); // FIXME
		return s.jump;
	}

	public static final void pushPos(PAsmContext px) {
		PAsmStack s = px.unused;
		s.tree = px; // FIXME
		s.longjmp = null; // FIXME
		s.pos = px.pos;
		push(px, s);
	}

	public static final int popPos(PAsmContext px) {
		PAsmStack s = pop(px);
		assert (s.tree == px); // FIXME
		return s.pos;
	}

	public static final void pushTree(PAsmContext px) {
		PAsmStack s = px.unused;
		s.pos = -2; // FIXME
		s.longjmp = null; // FIXME
		s.tree = px.tree;
		s.treeLog = px.treeLog;
		push(px, s);
	}

	public static final void popTree(PAsmContext px) {
		PAsmStack s = pop(px);
		assert (s.pos == -2); // FIXME
		px.tree = s.tree;
		px.treeLog = s.treeLog;
	}

	public static final void popTree(PAsmContext px, Symbol label) {
		PAsmStack s = pop(px);
		assert (s.pos == -2); // FIXME
		px.treeLog = s.treeLog;
		linkTree(px, label);
		px.tree = s.tree;
	}

	public static void pushState(PAsmContext px) {
		PAsmStack s = px.unused;
		s.pos = -3; // FIXME
		s.longjmp = null; // FIXME
		s.state = px.state;
		push(px, s);
	}

	public static void popState(PAsmContext px) {
		PAsmStack s = pop(px);
		assert (s.pos == -2); // FIXME
		px.state = s.state;
	}

	public static final void initVM(PAsmContext px) {
		px.unused = new PAsmStack(null);
		pushFail(px, new ASMexit(false));
		pushRet(px, new ASMexit(true));
	}

}
