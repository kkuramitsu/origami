package nez2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import nez2.Optimizer.Fold2;
import nez2.Optimizer.Tree2;
import nez2.PEG.Expr;
import nez2.PEG.PTag;

public interface Generator<X> {
	public X generate(String start, HashMap<String, Expr> nameMap, List<String> list);

	default void log(String msg) {
		System.out.println(msg);
	}
}

class ParserFuncGenerator implements Generator<Parser> {

	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;

	private int acc(Expr pe) {
		return POS | TREE;
		// return (Trees.isTree(pe)) ? POS | TREE : POS;
	}

	int mask = POS | TREE | STATE;

	HashMap<String, ParseFunc> funcMap = new HashMap<>();
	HashMap<String, Integer> recurMap = new HashMap<>();
	ParseFunc[] base = null;

	/* option */

	boolean checkEOF = false;
	boolean zeroTerm = true;
	boolean case4 = true;

	/* state */

	HashMap<String, Integer> stateMap = new HashMap<>();

	int stateId(Object label) {
		Integer n = this.stateMap.get(label.toString());
		if (n == null) {
			n = this.stateMap.size() + 1;
			this.stateMap.put(label.toString(), n);
		}
		return n;
	}

	@Override
	public Parser generate(String start, HashMap<String, Expr> nameMap, List<String> list) {
		int mp = 1;
		this.base = new ParseFunc[list.size()];
		for (String n : list) {
			Expr pe2 = nameMap.get(n);
			this.log("generating ... " + n + " = " + pe2);
			ParseFunc f = this.gen(pe2);
			if (Trees.isTree(pe2)) {
				this.funcMap.put(n, new MemoPoint(n, mp++, true, f));
			} else if (Trees.isUnit(pe2)) {
				this.funcMap.put(n, new MemoPoint(n, mp++, false, f));
			}
			// this.funcMap.put(n, f);
		}
		this.recurMap.forEach((n, x) -> {
			this.base[x] = this.funcMap.get(n);
			this.log("recur ... " + n + ", index=" + x + ", f=" + this.base[x]);
		});
		return new Parser(this.funcMap.get(start), this.funcMap, mp - 1);
	}

	/* Generator */

	HashMap<String, ParseFunc> cacheMap = new HashMap<>();

	ParseFunc gen(Expr pe) {
		ParseFunc f = this.gen2(pe);
		if (!pe.isNonTerm()) {
			String key = pe.toString();
			ParseFunc f2 = this.cacheMap.get(key);
			if (f2 == null) {
				this.cacheMap.put(key, f);
				f2 = f;
			}
			return f2;
		}
		return f;
	}

	ParseFunc gen2(Expr pe) {
		switch (pe.ptag) {
		case Empty:
			return ParserFuncGenerator::succ;
		case Char:
			return this.cChar((BitChar) pe.param(0));
		case NonTerm: {
			String name = pe.p(0);
			ParseFunc f = this.funcMap.get(name);
			if (f != null) {
				return f;
			}
			Integer rec = this.recurMap.get(name);
			if (rec == null) {
				rec = this.recurMap.size();
				this.recurMap.put(name, rec);
			}
			return cRef(this.base, rec);
		}
		case Seq: {
			Expr[] es = pe.flatten(PTag.Seq);
			byte[] b = PEG.getstr2(es);
			if (b.length > 1) {
				ParseFunc f = this.cCharInc(b);
				if (b.length == es.length) {
					return f;
				}
				ParseFunc[] fs = new ParseFunc[es.length - b.length + 1];
				fs[0] = f;
				for (int i = 0; i < fs.length - 1; i++) {
					fs[i + 1] = this.gen(es[b.length + i]);
				}
				return this.cAdd(fs);
			}
			return this.cAdd(this.gen(es));
		}
		case Or:
			if (pe.get(1).isEmpty()) {
				return this.cOption(pe.get(0));
			}
			return this.cChoice(this.acc(pe), this.gen(pe.flatten(PTag.Or)));
		case Alt:
			return this.cChoice(this.acc(pe), this.gen(pe.flatten(PTag.Alt))); // FIXME
		case And:
			return cAnd(this.gen(pe));
		case Not:
			return this.cNot(pe.get(0));
		case Many:
			return this.cMany(pe.get(0));
		case OneMore:
			return cAdd2(this.gen(pe.get(0)), this.cMany(pe.get(0)));
		/* */
		case Tree: {
			if (pe instanceof Tree2) {
				Tree2 t = (Tree2) pe;
				return cTree(this.gen(pe.get(0)), t.spos, t.tag == null ? TreeNode.EmptyTag : t.tag, t.epos);
			}
			return cTree(this.gen(pe.get(0)));
		}
		case Link: {
			return cLink(this.gen(pe.get(0)), pe.p(0));
		}
		case Fold: {
			if (pe instanceof Fold2) {
				Fold2 t = (Fold2) pe;
				return cFold(this.gen(pe.get(0)), pe.p(0), t.spos, t.tag == null ? TreeNode.EmptyTag : t.tag, t.epos);
			}
			return cFold(this.gen(pe.get(0)), pe.p(0), 0, TreeNode.EmptyTag, 0);
		}
		case Tag:
			return cTag((String) pe.param(0));
		case Val:
			return ParserFuncGenerator::succ;
		/* */
		case Scope: /* @symbol(A) */
			return cScope(this.gen(pe.get(0)));
		case Symbol: /* @symbol(A) */
			return cSymbol(this.gen(pe.get(0)), this.stateId(pe.param(0)));
		case Exists:
			return cExists(this.stateId(pe.param(0)));
		case Match:
			return cMatch(this.stateId(pe.param(0)));
		case Contains:
		case Equals:
		case Eval:
			return ParserFuncGenerator::succ;
		/* */
		case DFA:
			return this.cDFA((DFA) pe);
		/* */
		case If: /* @if(flag, e) */
		case On: /* @on(flag, e) */
		case Off: /* @off(flag, e) */
		default:
			System.err.printf("TODO(gen, %s)\n", pe);
			return ParserFuncGenerator::succ;
		}
	}

	ParseFunc[] gen(Expr[] pe) {
		return Arrays.stream(pe).map(e -> this.gen(e)).toArray(ParseFunc[]::new);
	}

	/* BitChar */

	byte[] zeroTerm(byte[] indexMap) {
		if (this.zeroTerm && indexMap[0] != 0) {
			byte[] b = indexMap.clone();
			b[0] = 0;
			return b;
		}
		return indexMap;
	}

	BitChar zeroTerm(BitChar bc) {
		return (this.zeroTerm) ? bc.textVersion() : bc;
	}

	private ParseFunc cDFA(DFA pe) {
		final ParseFunc[] f = new ParseFunc[pe.indexed.length + 1];
		f[0] = ParserFuncGenerator::fail;
		if (pe.isDFA()) {
			for (int i = 0; i < pe.indexed.length; i++) {
				f[i + 1] = this.gen(DFA.cdr(pe.indexed[i]));
			}
			return cDFA(f, this.zeroTerm(pe.charMap));
		} else {
			for (int i = 0; i < pe.indexed.length; i++) {
				f[i + 1] = this.gen(pe.indexed[i]);
			}
			return cNFA(f, this.zeroTerm(pe.charMap));
		}
	}

	static ParseFunc cDFA(final ParseFunc[] f, final byte[] indexMap) {
		if (f.length > 127) {
			return (px) -> f[indexMap[px.inputs[px.pos++] & 0xff] & 0xff].apply(px);
		} else {
			return (px) -> f[indexMap[px.inputs[px.pos++] & 0xff]].apply(px);
		}
	}

	static ParseFunc cNFA(final ParseFunc[] f, final byte[] indexMap) {
		if (f.length > 127) {
			return (px) -> f[indexMap[px.inputs[px.pos] & 0xff] & 0xff].apply(px);
		} else {
			return (px) -> f[indexMap[px.inputs[px.pos] & 0xff]].apply(px);
		}
	}

	private ParseFunc cChar(BitChar bs) {
		if (bs.isSingle()) {
			return this.cCharInc(bs.single());
		}
		int[] bits = this.zeroTerm(bs).bits();
		// int n = 0xe3;
		// System.err.printf("<<bits %s %s %x %s>>", bs, bits, n, (bits[n / 32] & (1 <<
		// (n % 32))) != 0);
		return this.cCharSetInc(bits);
	}

	// generator library

	// Obits32
	private final static boolean bits32(int[] bits, byte b) {
		int n = b & 0xff;
		// System.err.printf("$$$ %s %x %s ", bits, n, (bits[n / 32] & (1 << (n % 32)))
		// != 0);
		return (bits[n / 32] & (1 << (n % 32))) != 0;
	}

	// match
	final static boolean matchmany(byte[] inputs, int pos, byte[] inputs2, int pos2, int len) {
		for (int i = 0; i < len; i++) {
			if (inputs[pos + i] != inputs2[pos2 + i]) {
				return false;
			}
		}
		return true;
	}

	// errpos
	final static int mbackpos(ParserContext px, int pos) {
		if (px.headpos < px.pos) {
			px.headpos = px.pos;
		}
		return pos;
	}

	private final static boolean mmov(ParserContext px, int pos) {
		px.pos = px.pos + pos;
		return px.pos < px.length;
	}

	private final static boolean mback1(ParserContext px, int pos) {
		px.pos = mbackpos(px, pos);
		return true;
	}

	private final static boolean mback3(ParserContext px, int pos, T tree) {
		px.pos = mbackpos(px, pos);
		px.tree = tree;
		return true;
	}

	private final static boolean mback4(ParserContext px, State state) {
		px.state = state;
		return true;
	}

	final static boolean mback7(ParserContext px, int pos, T tree, State state) {
		px.pos = mbackpos(px, pos);
		px.tree = tree;
		px.state = state;
		return true;
	}

	static boolean succ(ParserContext px) {
		return true;
	}

	static boolean fail(ParserContext px) {
		return false;
	}

	static ParseFunc cRef(final ParseFunc[] base, int index) {
		assert (base != null);
		return (px) -> {
			return base[index].apply(px);
		};
	}

	ParseFunc cCharInc(final byte b) {
		if (this.checkEOF || b == 0) {
			System.err.println("EOF " + (int) b);
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos++] == b;
			};
		}
		return (px) -> {
			return px.inputs[px.pos++] == b;
		};
	}

	ParseFunc cChar(final byte b) {
		if (this.checkEOF || b == 0) {
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos] == b;
			};
		}
		return (px) -> {
			return px.inputs[px.pos] == b;
		};
	}

	ParseFunc cChar(final byte b, final byte b2) {
		if (this.checkEOF || b == 0 || b2 == 0) {
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2;
			};
		}
		return (px) -> {
			return px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2;
		};
	}

	ParseFunc cChar(final byte b, final byte b2, final byte b3) {
		if (this.checkEOF || b == 0 || b2 == 0 || b3 == 0) {
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2
						&& px.inputs[px.pos + 2] == b3;
			};
		}
		return (px) -> {
			return px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2 && px.inputs[px.pos + 2] == b3;
		};
	}

	ParseFunc cChar(final byte b, final byte b2, final byte b3, final byte b4) {
		if (this.checkEOF || b == 0 || b2 == 0 || b3 == 0 || b4 == 0) {
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2
						&& px.inputs[px.pos + 2] == b3 && px.inputs[px.pos + 3] == b4;
			};
		}
		return (px) -> {
			return px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2 && px.inputs[px.pos + 2] == b3
					&& px.inputs[px.pos + 3] == b4;
		};
	}

	ParseFunc cChar(final byte[] b) {
		final int len = b.length;
		if (this.case4) {
			switch (len) {
			case 0:
				return ParserFuncGenerator::succ;
			case 1:
				return this.cChar(b[0]);
			case 2:
				return this.cChar(b[0], b[1]);
			case 3:
				return this.cChar(b[0], b[1], b[2]);
			case 4:
				return this.cChar(b[0], b[1], b[2], b[3]);
			default:
			}
		}
		if (this.checkEOF) {
			return (px) -> px.pos + len < px.length && matchmany(px.inputs, px.pos, b, 0, len);
		}
		return (px) -> matchmany(px.inputs, px.pos, b, 0, len);
	}

	ParseFunc cMov(final ParseFunc f, final int len) {
		return (px) -> {
			if (f.apply(px)) {
				px.pos += len;
				return true;
			}
			return false;
		};
	}

	ParseFunc cCharInc(final byte[] b) {
		return this.cMov(this.cChar(b), b.length);
	}

	ParseFunc cCharSet(final int[] bs) {
		if (this.checkEOF || bits32(bs, (byte) 0)) {
			return (px) -> {
				return px.pos < px.length && bits32(bs, px.inputs[px.pos]);
			};
		}
		return (px) -> {
			return bits32(bs, px.inputs[px.pos]);
		};
	}

	ParseFunc cCharSetInc(final int[] bs) {
		if (this.checkEOF || bits32(bs, (byte) 0)) {
			return (px) -> {
				return px.pos < px.length && bits32(bs, px.inputs[px.pos++]);
			};
		}
		return (px) -> {
			return bits32(bs, px.inputs[px.pos++]);
		};
	}

	static ParseFunc cAdd2(ParseFunc f, ParseFunc f2) {
		return (px) -> {
			return f.apply(px) && f2.apply(px);
		};
	}

	static ParseFunc cAdd3(ParseFunc f, ParseFunc f2, ParseFunc f3) {
		return (px) -> {
			return f.apply(px) && f2.apply(px) && f3.apply(px);
		};
	}

	static ParseFunc cAdd4(ParseFunc f, ParseFunc f2, ParseFunc f3, ParseFunc f4) {
		return (px) -> {
			return f.apply(px) && f2.apply(px) && f3.apply(px) && f4.apply(px);
		};
	}

	ParseFunc cAdd(final ParseFunc... fs) {
		if (this.case4) {
			switch (fs.length) {
			case 0:
				return ParserFuncGenerator::succ;
			case 1:
				return fs[0];
			case 2:
				return cAdd2(fs[0], fs[1]);
			case 3:
				return cAdd3(fs[0], fs[1], fs[2]);
			case 4:
				return cAdd4(fs[0], fs[1], fs[2], fs[3]);
			}
		}
		// System.err.println("TODO fs.length=" + fs.length);
		final int tail = fs.length - 1;
		return (px) -> {
			for (int i = 0; i < tail; i++) {
				if (!fs[i].apply(px)) {
					return false;
				}
			}
			return fs[tail].apply(px);
		};
	}

	ParseFunc cSeq(Expr[] es) {
		byte[] b = PEG.getstr2(es);
		if (b.length > 1) {
			ParseFunc f = this.cCharInc(b);
			if (b.length == es.length) {
				return f;
			}
			ParseFunc[] fs = new ParseFunc[es.length - b.length + 1];
			fs[0] = f;
			for (int i = 0; i < fs.length - 1; i++) {
				fs[i + 1] = this.gen(es[b.length + i]);
			}
			return this.cAdd(fs);
		}
		return this.cAdd(this.gen(es));
	}

	static ParseFunc cOption(int acc, final ParseFunc f) {
		if (acc == 1) {
			return (px) -> {
				int pos = px.pos;
				return f.apply(px) || mback1(px, pos);
			};
		}
		if (acc == 3) {
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				return f.apply(px) || mback3(px, pos, tree);
			};
		}
		return (px) -> {
			int pos = px.pos;
			T tree = px.tree;
			State state = px.state;
			return f.apply(px) || mback7(px, pos, tree, state);
		};
	}

	final static ParseFunc fOptionAny = (px) -> {
		if (px.pos < px.length) {
			px.pos = px.pos + 1;
		}
		return true;
	};

	ParseFunc cOption(final byte b) {
		if (this.checkEOF || b == 0) {
			return (px) -> {
				if (px.pos < px.length && px.inputs[px.pos] == b) {
					px.pos = px.pos + 1;
				}
				return true;
			};
		}
		return (px) -> {
			if (px.inputs[px.pos] == b) {
				px.pos = px.pos + 1;
			}
			return true;
		};
	}

	ParseFunc cOption(final int[] bm) {
		if (this.checkEOF || bits32(bm, (byte) 0)) {
			return (px) -> {
				if (px.pos < px.length && bits32(bm, px.inputs[px.pos])) {
					px.pos = px.pos + 1;
				}
				return true;
			};
		}
		return (px) -> {
			if (bits32(bm, px.inputs[px.pos])) {
				px.pos = px.pos + 1;
			}
			return true;
		};
	}

	ParseFunc cOption(final byte[] b) {
		final int len = b.length;
		if (this.checkEOF) {
			return (px) -> {
				if (px.pos + len < px.length && matchmany(px.inputs, px.pos, b, 0, len)) {
					px.pos = px.pos + len;
				}
				return true;
			};
		} else {
			return (px) -> {
				if (matchmany(px.inputs, px.pos, b, 0, len)) {
					px.pos = px.pos + len;
				}
				return true;
			};
		}
	}

	private ParseFunc cOption(Expr pe) {
		Expr inner = pe.deref();
		if (inner.isChar()) {
			BitChar bc = (BitChar) pe.param(0);
			if (bc.isSingle()) {
				return this.cOption(bc.single());
			}
			if (bc.isAny()) {
				return fOptionAny;
			}
			return this.cOption(this.zeroTerm(bc).bits());
		}
		if (inner.ptag == PTag.Seq) {
			Expr[] es = inner.flatten(PTag.Seq);
			byte[] b = PEG.getstr2(es);
			if (es.length == b.length) {
				return this.cOption(b);
			}
		}
		return cOption(this.acc(pe), this.gen(pe));
	}

	static ParseFunc cChoice2(int acc, ParseFunc f, ParseFunc f2) {
		if (acc == 1) {
			return (px) -> {
				int pos = px.pos;
				return f.apply(px) || mback1(px, pos) && f2.apply(px);
			};
		}
		if (acc == 3) {
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				return f.apply(px) || mback3(px, pos, tree) && f2.apply(px);
			};
		}
		return (px) -> {
			int pos = px.pos;
			T tree = px.tree;
			State state = px.state;
			return f.apply(px) || mback7(px, pos, tree, state) && f2.apply(px);
		};
	}

	static ParseFunc cChoice3(int acc, ParseFunc f, ParseFunc f2, ParseFunc f3) {
		if (acc == 1) {
			return (px) -> {
				int pos = px.pos;
				return f.apply(px) || mback1(px, pos) && f2.apply(px) || mback1(px, pos) && f3.apply(px);
			};
		}
		if (acc == 3) {
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				return f.apply(px) || mback3(px, pos, tree) && f2.apply(px) || mback3(px, pos, tree) && f3.apply(px);
			};
		}
		return (px) -> {
			int pos = px.pos;
			T tree = px.tree;
			State state = px.state;
			return f.apply(px) || mback7(px, pos, tree, state) && f2.apply(px)
					|| mback7(px, pos, tree, state) && f3.apply(px);
		};
	}

	static ParseFunc cChoice4(int acc, ParseFunc f, ParseFunc f2, ParseFunc f3, ParseFunc f4) {
		if (acc == 1) {
			return (px) -> {
				int pos = px.pos;
				return f.apply(px) || mback1(px, pos) && f2.apply(px) || mback1(px, pos) && f3.apply(px)
						|| mback1(px, pos) && f4.apply(px);
			};
		}
		if (acc == 3) {
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				return f.apply(px) || mback3(px, pos, tree) && f2.apply(px) || mback3(px, pos, tree) && f3.apply(px)
						|| mback3(px, pos, tree) && f4.apply(px);
			};
		}
		return (px) -> {
			int pos = px.pos;
			T tree = px.tree;
			State state = px.state;
			return f.apply(px) || mback7(px, pos, tree, state) && f2.apply(px)
					|| mback7(px, pos, tree, state) && f3.apply(px) || mback7(px, pos, tree, state) && f4.apply(px);
		};
	}

	ParseFunc cChoice(int acc, final ParseFunc... fs) {
		if (this.case4) {
			switch (fs.length) {
			case 0:
				return ParserFuncGenerator::fail;
			case 1:
				return fs[0];
			case 2:
				return cChoice2(acc, fs[0], fs[1]);
			case 3:
				return cChoice3(acc, fs[0], fs[1], fs[2]);
			case 4:
				return cChoice4(acc, fs[0], fs[1], fs[2], fs[3]);
			}
		}
		final int tail = fs.length - 1;
		if (acc == 1) {
			return (px) -> {
				int pos = px.pos;
				for (int i = 0; i < tail; i++) {
					if (fs[i].apply(px)) {
						return true;
					}
					px.pos = pos;
				}
				return fs[tail].apply(px);
			};
		}
		if (acc == 3) {
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				for (int i = 0; i < tail; i++) {
					if (fs[i].apply(px)) {
						return true;
					}
					px.pos = pos;
					px.tree = tree;
				}
				return fs[tail].apply(px);
			};
		}
		return (px) -> {
			int pos = px.pos;
			T tree = px.tree;
			State state = px.state;
			for (int i = 0; i < tail; i++) {
				if (fs[i].apply(px)) {
					return true;
				}
				px.pos = pos;
				px.tree = tree;
				px.state = state;
			}
			return fs[tail].apply(px);
		};
	}

	/* Many */

	static ParseFunc cMany(int acc, ParseFunc f) {
		if (acc == 1) {
			return (px) -> {
				int pos = px.pos;
				while (f.apply(px)) {
					pos = px.pos;
				}
				return mback1(px, pos);
			};
		}
		if (acc == 3) {
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				while (f.apply(px)) {
					pos = px.pos;
					tree = px.tree;
				}
				return mback3(px, pos, tree);
			};
		}
		return (px) -> {
			int pos = px.pos;
			T tree = px.tree;
			State state = px.state;
			while (f.apply(px)) {
				pos = px.pos;
				tree = px.tree;
				state = px.state;
			}
			return mback7(px, pos, tree, state);
		};
	}

	static ParseFunc cManyX(int acc, ParseFunc f) {
		if (acc == 1) {
			return (px) -> {
				int pos = px.pos;
				while (f.apply(px) && pos < px.pos) {
					pos = px.pos;
				}
				return mback1(px, pos);
			};
		}
		if (acc == 3) {
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				while (f.apply(px) && pos < px.pos) {
					pos = px.pos;
					tree = px.tree;
				}
				return mback3(px, pos, tree);
			};
		}
		return (px) -> {
			int pos = px.pos;
			T tree = px.tree;
			State state = px.state;
			while (f.apply(px) && pos < px.pos) {
				pos = px.pos;
				tree = px.tree;
				state = px.state;
			}
			return mback7(px, pos, tree, state);
		};
	}

	final static ParseFunc fManyAny = (px) -> {
		if (px.pos < px.length) {
			px.pos = px.length;
		}
		return true;
	};

	ParseFunc cMany(final byte b) {
		if (this.checkEOF || b == 0) {
			return (px) -> {
				while (px.pos < px.length && px.inputs[px.pos] == b) {
					px.pos = px.pos + 1;
				}
				return true;
			};
		}
		return (px) -> {
			while (px.inputs[px.pos] == b) {
				px.pos = px.pos + 1;
			}
			return true;
		};
	}

	ParseFunc cMany(final int[] bm) {
		if (this.checkEOF || bits32(bm, (byte) 0)) {
			return (px) -> {
				while (px.pos < px.length && bits32(bm, px.inputs[px.pos])) {
					px.pos = px.pos + 1;
				}
				return true;
			};
		}
		return (px) -> {
			while (bits32(bm, px.inputs[px.pos])) {
				px.pos = px.pos + 1;
			}
			return true;
		};
	}

	ParseFunc cMany(final byte[] b) {
		final int len = b.length;
		if (this.checkEOF) {
			return (px) -> {
				while (px.pos + len < px.length && matchmany(px.inputs, px.pos, b, 0, len)) {
					px.pos = px.pos + len;
				}
				return true;
			};
		} else {
			return (px) -> {
				while (matchmany(px.inputs, px.pos, b, 0, len)) {
					px.pos = px.pos + len;
				}
				return true;
			};
		}
	}

	private ParseFunc cMany(Expr pe) {
		Expr inner = pe.deref();
		if (inner.isChar()) {
			BitChar bc = (BitChar) pe.param(0);
			if (bc.isSingle()) {
				return this.cMany(bc.single());
			}
			if (bc.isAny()) {
				return fManyAny;
			}
			return this.cMany(this.zeroTerm(bc).bits());
		}
		if (inner.ptag == PTag.Seq) {
			Expr[] es = inner.flatten(PTag.Seq);
			byte[] b = PEG.getstr2(es);
			if (es.length == b.length) {
				return this.cMany(b);
			}
		}
		if (pe.isNullable()) {
			return cManyX(this.acc(pe), this.gen(pe));
		}
		return cMany(this.acc(pe), this.gen(pe));
	}

	static ParseFunc cAnd(ParseFunc f) {
		return (px) -> {
			int pos = px.pos;
			return f.apply(px) || mback1(px, pos);
		};
	}

	/* Not */

	static ParseFunc cNot(int acc, ParseFunc f) {
		switch (acc) {
		case 1:
			return (px) -> {
				int pos = px.pos;
				return !f.apply(px) || mback1(px, pos);
			};
		case 3:
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				return !f.apply(px) || mback3(px, pos, tree);
			};
		default:
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				State state = px.state;
				return !(f.apply(px)) && mback7(px, pos, tree, state);
			};
		}
	}

	final static ParseFunc fNotAny = (px) -> {
		return px.pos >= px.length;
	};

	ParseFunc cNot(final byte b) {
		if (this.checkEOF || b == 0) {
			return (px) -> !(px.pos < px.length && px.inputs[px.pos] == b);
		}
		return (px) -> (px.inputs[px.pos] != b);
	}

	ParseFunc cNot(final int[] bm) {
		if (this.checkEOF || bits32(bm, (byte) 0)) {
			return (px) -> !(px.pos < px.length && bits32(bm, px.inputs[px.pos]));
		}
		return (px) -> !bits32(bm, px.inputs[px.pos]);
	}

	ParseFunc cNot2(final byte b0, final byte b1) {
		if (this.checkEOF || b0 == 0 || b1 == 0) {
			return (px) -> !(px.pos + 1 < px.length && px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1);
		} else {
			return (px) -> !(px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1);
		}
	}

	ParseFunc cNot3(final byte b0, final byte b1, final byte b2) {
		if (this.checkEOF || b0 == 0 || b1 == 0 || b2 == 0) {
			return (px) -> !(px.pos + 2 < px.length && px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1
					&& px.inputs[px.pos + 2] == b2);
		}
		return (px) -> !(px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1 && px.inputs[px.pos + 2] == b2);
	}

	ParseFunc cNot(final byte[] b) {
		if (this.case4) {
			switch (b.length) {
			case 1:
				return this.cNot(b[0]);
			case 2:
				return this.cNot2(b[0], b[1]);
			case 3:
				return this.cNot3(b[0], b[1], b[2]);
			}
		}
		final int len = b.length;
		if (this.checkEOF) {
			return (px) -> !(px.pos + len < px.length && matchmany(px.inputs, px.pos, b, 0, len));
		}
		return (px) -> !matchmany(px.inputs, px.pos, b, 0, len);
	}

	private ParseFunc cNot(Expr pe) {
		Expr inner = pe.deref();
		if (inner.isChar()) {
			BitChar bc = (BitChar) pe.param(0);
			if (bc.isSingle()) {
				return this.cNot(bc.single());
			}
			if (bc.isAny()) {
				return fNotAny;
			}
			return this.cNot(this.zeroTerm(bc).bits());
		}
		if (inner.ptag == PTag.Seq) {
			Expr[] es = inner.flatten(PTag.Seq);
			byte[] b = PEG.getstr2(es);
			if (es.length == b.length) {
				return this.cNot(b);
			}
		}
		return cNot(this.acc(pe), this.gen(pe));
	}

	/* TreeConstruction */

	private final static boolean mtree(ParserContext px, String tag, int spos, int epos) {
		px.tree = new TreeNode(tag, px.inputs, spos, epos, px.tree);
		return true;
	}

	private final static boolean mlink(ParserContext px, String tag, T child, T prev) {
		px.tree = new TreeLink(tag, child, prev);
		return true;
	}

	static ParseFunc cTree(final ParseFunc f) {
		return (px) -> {
			int pos = px.pos;
			px.tree = null;
			return f.apply(px) && mtree(px, TreeNode.EmptyTag, pos, px.pos);
		};
	}

	static ParseFunc cTree(final ParseFunc f, final int spos, final String tag, final int epos) {
		return (px) -> {
			int pos = px.pos;
			px.tree = null;
			return f.apply(px) && mtree(px, tag, pos + spos, px.pos + epos);
		};
	}

	static ParseFunc cLink(ParseFunc f, String tag) {
		return (px) -> {
			T tree = px.tree;
			return f.apply(px) && mlink(px, tag, px.tree, tree);
		};
	}

	static ParseFunc cFold(final ParseFunc f, final String label) {
		return (px) -> {
			int pos = px.pos;
			return mlink(px, label, px.tree, null) && f.apply(px) && mtree(px, TreeNode.EmptyTag, pos, px.pos);
		};
	}

	static ParseFunc cFold(final ParseFunc f, final String label, final int spos, final String tag, final int epos) {
		return (px) -> {
			int pos = px.pos;
			return mlink(px, label, px.tree, null) && f.apply(px) && mtree(px, tag, pos + spos, px.pos + epos);
		};
	}

	static ParseFunc cTag(final String tag) {
		return (px) -> {
			return mlink(px, tag, null, px.tree);
		};
	}

	private final static int memosize = 128;

	private final static long getkey(int pos, int mp) {
		return pos * memosize + mp;
	}

	private final static MemoEntry getmemo(ParserContext px, long key) {
		return px.memos[(int) (key % px.memos.length)];
	}

	private final static boolean mconsume1(ParserContext px, MemoEntry memo) {
		px.pos = memo.mpos;
		px.state = memo.mstate;
		return memo.matched;
	}

	private final static boolean mconsume3(ParserContext px, MemoEntry memo) {
		px.pos = memo.mpos;
		px.tree = memo.mtree;
		px.state = memo.mstate;
		return memo.matched;
	}

	private final static boolean mstore3(ParserContext px, MemoEntry memo, long key, boolean matched) {
		memo.key = key;
		memo.mpos = px.pos;
		memo.mtree = px.tree;
		memo.mstate = px.state;
		memo.matched = matched;
		return matched;
	}

	ParseFunc cMemo(ParseFunc f, int mp) {
		return (px) -> {
			int pos = px.pos;
			long key = getkey(pos, mp);
			MemoEntry memo = getmemo(px, key);
			return (memo.key == key) ? mconsume3(px, memo) : mstore3(px, memo, key, f.apply(px));
		};
	}

	static class MemoPoint implements ParseFunc {
		String name;
		final int mp;
		final ParseFunc f;
		final boolean tree;
		boolean unused;
		int hit;
		int memoed;

		MemoPoint(String name, int mp, boolean tree, ParseFunc f) {
			this.name = name;
			this.mp = mp;
			this.tree = tree;
			this.f = f;
		}

		void reset() {
			this.unused = false;
			this.hit = 0;
			this.memoed = 0;
		}

		@Override
		public boolean apply(ParserContext px) {
			if (this.unused) {
				return this.f.apply(px);
			} else {
				int pos = px.pos;
				long key = getkey(pos, this.mp);
				MemoEntry memo = getmemo(px, key);
				if (memo.key == key && memo.mstate == px.state) {
					this.hit++;
					px.pos = memo.mpos;
					px.state = memo.mstate;
					if (this.tree) {
						px.tree = memo.mtree;
					}
					return memo.matched;
				}
				this.memoed++;
				if (this.memoed == 101) {
					if (this.hit < 10) {
						this.unused = true;
					}
				}
				return mstore3(px, memo, key, this.f.apply(px));
			}
		}

		@Override
		public String toString() {
			return String.format("%s#%d %c %d/%d %f%%", this.name, this.mp, this.tree ? 'T' : 'U', this.hit,
					this.memoed, this.hit * 100.0 / this.memoed);
		}
	}

	private final static boolean mstate(ParserContext px, int ns, int pos) {
		px.state = new State(ns, pos, px.pos, px.state);
		return true;
	}

	static ParseFunc cScope(ParseFunc pe) {
		return (px) -> {
			State state = px.state;
			return pe.apply(px) && mback4(px, state);
		};
	}

	static ParseFunc cSymbol(ParseFunc pe, int ns) {
		return (px) -> {
			int pos = px.pos;
			return pe.apply(px) && mstate(px, ns, pos);
		};
	}

	final static State getstate(State state, int ns) {
		return (state == null || state.ns == ns) ? state : getstate(state.sprev, ns);
	}

	static ParseFunc cMatch(int ns) {
		return (px) -> {
			State state = getstate(px.state, ns);
			return state != null && matchmany(px.inputs, px.pos, px.inputs, state.spos, state.slen)
					&& mmov(px, state.slen);
		};
	}

	static ParseFunc cExists(int ns) {
		return (px) -> getstate(px.state, ns) != null;
	}

}
