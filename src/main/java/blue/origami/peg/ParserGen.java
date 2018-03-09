package blue.origami.peg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

import blue.origami.peg.PEG.CTag;
import blue.origami.peg.PEG.Expr;
import blue.origami.peg.PEG.Memoed;
import blue.origami.peg.PEG.NonTerm;

class ParserGen {

	boolean isBinary = false;
	int mask;
	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;

	private int acc(Expr pe) {
		return POS | TREE | STATE;
	}

	HashMap<String, Object> memoed = new HashMap<>();

	Parser generate(String start, Expr pe) {
		HashMap<String, Expr> prodMap = new HashMap<>();
		prodMap.put(start, pe);
		TreeSet<String> flagSet = new TreeSet<>();
		this.makeDict(start, pe, prodMap, flagSet);
		System.out.println("start: " + start + " prodMap: " + prodMap);
		if (flagSet.size() > 0) {
			System.out.println("flagSet: " + flagSet);
		}
		HashMap<String, NonTerm2> nameMap = new HashMap<>();
		NonTerm2 snt = new NonTerm2(start, null, PEG.Empty_);
		nameMap.put(start, snt);
		snt.inner = this.rename(pe, prodMap, nameMap, new Flags());
		nameMap.forEach((name, nt) -> {
			checkLeftRecur(name, nt.get(0));
			nt.inner = trace("ast", name, nt.get(0), (p) -> Trees.checkAST(p));
		});
		// 1. optimizing ..
		// nameMap.forEach((name, nt) -> {
		// nt.inner = optimize(nt.get(0));
		// });
		nameMap.forEach((name, nt) -> {
			nt.inner = trace("inline", name, nt.get(0), (p) -> inline(p));
		});

		// 2. generating ..
		HashMap<String, NonTerm2> nameMap2 = new HashMap<>();
		nameMap2.put(start, snt);
		this.makeDict2(start, snt.get(0), nameMap2);
		System.out.println("size: " + nameMap.size() + " => " + nameMap2.size());

		HashSet<String> crossRefs = new HashSet<>();
		List<String> list = this.sortList(nameMap2, crossRefs);
		if (!list.contains(start)) {
			list.add(start);
		}
		System.out.println("list: " + list);
		System.out.println("crossrefs: " + crossRefs);
		GeneratorContext gx = new GeneratorContext(this.isBinary, list.size());
		for (String n : list) {
			System.out.println("generating ... " + n + " " + nameMap2.get(n).get(0));
			gx.funcMap.put(n, this.gen(nameMap2.get(n).get(0), gx));
		}
		gx.recurMap.forEach((n, x) -> {
			gx.base[x] = gx.funcMap.get(n);
			// System.out.println("recur ... " + n + ", index=" + x + ", f=" + gx.base[x]);
		});
		return new Parser(gx.funcMap.get(start), gx.funcMap, 0);
	}

	static Expr trace(String p, String name, Expr pe, Function<Expr, Expr> f) {
		Expr pe2 = f.apply(pe);
		if (!pe.toString().equals(pe2.toString())) {
			System.err.printf("modified %s %s\n\t%s\n\t=> %s\n", p, name, pe, pe2);
		}
		return pe2;
	}

	void makeDict(String curName, Expr pe, HashMap<String, Expr> prodMap, TreeSet<String> flagSet) {
		if (pe != null) {
			switch (pe.ctag) {
			case NonTerm:
				Expr deref = pe.get(0);
				if (deref != null) {
					String key = this.uname(pe);
					if (!prodMap.containsKey(key)) {
						prodMap.put(key, deref);
						this.makeDict(key, deref, prodMap, flagSet);
					}
				}
				return;
			case Char:
				if (((BitChar) pe.param(0)).isBinary()) {
					this.isBinary = true;
				}
				return;
			case If:
				flagSet.add((String) pe.param(0));
				return;
			default:
				this.makeDict(curName, pe.get(0), prodMap, flagSet);
				this.makeDict(curName, pe.get(1), prodMap, flagSet);
			}
		}
	}

	private final String uname(Expr pe) {
		return pe.p(0);
	}

	Expr rename(Expr pe, HashMap<String, Expr> prodMap, HashMap<String, NonTerm2> nameMap, Flags flags) {
		switch (pe.ctag) {
		case NonTerm: {
			Expr inner = pe.get(0);
			assert !(pe instanceof NonTerm2);
			if (inner != null) {
				String key = this.uname(pe);
				String uname = flags.uname(key, (Memoed) pe);
				NonTerm2 nt = nameMap.get(uname);
				if (nt == null) {
					nt = new NonTerm2(uname, (String[]) pe.param(1), PEG.Empty_);
					nameMap.put(uname, nt);
					nt.inner = this.rename(inner, prodMap, nameMap, flags);
				}
				return nt;
			}
			return new Var(pe.p(0), ((NonTerm) pe).index);
		}
		case App: {
			Expr f = this.rename(pe.get(0), prodMap, nameMap, flags);
			if (!f.isApp()) {
				return f;
			}

		}

		case If:
			if (flags.is(pe.p(0))) {
				return PEG.Empty_;
			}
			return PEG.Fail_;
		case On: {
			boolean stacked = flags.is(pe.p(0));
			flags.set(pe.p(0), true);
			Expr inner = this.rename(pe.get(0), prodMap, nameMap, flags);
			flags.set(pe.p(0), stacked);
			return inner;
		}
		case Off: {
			boolean stacked = flags.is(pe.p(0));
			flags.set(pe.p(0), false);
			Expr inner = this.rename(pe.get(0), prodMap, nameMap, flags);
			flags.set(pe.p(0), stacked);
			return inner;
		}
		default:
			return PEG.dup(pe, (p) -> this.rename(p, prodMap, nameMap, flags));
		}
	}

	class NonTerm2 extends Memoed {
		Expr inner;
		String[] params;

		public NonTerm2(String name, String params[], Expr inner) {
			this.ctag = CTag.NonTerm;
			this.memoed = ParserGen.this.memoed;
			this.label = name;
			this.inner = inner;
			this.params = params;
		}

		@Override
		public Expr get(int index) {
			return index == 0 ? this.inner : null;
		}

		@Override
		public Object param(int index) {
			if (index == 0) {
				return this.label;
			}
			return this.params;
		}

		@Override
		public int psize() {
			return 2;
		}

	}

	@SuppressWarnings("serial")
	static class Flags extends TreeSet<String> {
		boolean is(String flag) {
			return this.contains(flag);
		}

		void set(String flag, boolean on) {
			if (on) {
				this.add(flag);
			} else {
				this.remove(flag);
			}
		}

		String uname(String uname, Memoed memo) {
			if (this.size() == 0) {
				return uname;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(uname);
			for (String flag : this) {
				if (First.reachFlag(memo, flag)) {
					sb.append("&");
					sb.append(flag);
				}
			}
			return sb.toString();
		}
	}

	// check

	static void checkLeftRecur(NonTerm nt) {
		try {
			checkLeftRecur(nt.p(0), nt.get(0));
		} catch (Exception e) {
			nt.peg.log("left recursion %s", nt);
		}
	}

	static boolean checkLeftRecur(String name, Expr pe) {
		switch (pe.ctag) {
		case NonTerm:
			if (name.equals(pe.param(0))) {
				((NonTerm) pe).peg.log("left recursion %s", name);
				((NonTerm) pe).label = name + '\'';
				return true;
			}
			return checkLeftRecur(name, pe.get(0));
		case Char:
			return true;
		case Seq:
			return checkLeftRecur(name, pe.get(0)) || checkLeftRecur(name, pe.get(1));
		case Or:
		case Alt:
			return checkLeftRecur(name, pe.get(0)) || checkLeftRecur(name, pe.get(1));
		case Empty:
		case Tag:
		case Val:
		case If:
		case Exists:
			return false;
		case And:
		case Not:
		case Many:
			checkLeftRecur(name, pe.get(0));
			return false;
		case OneMore:
		case Tree:
		case Link:
		case Fold:
		case Untree:
		case Scope:
		case Symbol:
		case Match:
		case Equals:
		case Contains:
		case On:
		case Off:
			return checkLeftRecur(name, pe.get(0));
		default:
			System.err.println("ERR left " + pe);
			return true;
		}

	}

	// Elimination

	// Optimized

	// Expr opt(Expr pe) {
	// if (pe instanceof Seq) {
	// Expr[] seq = pe.flatten();
	// }
	// }
	//
	// static Expr expand(Expr pe) {
	// switch (pe.ctag) {
	// case Seq:
	// return this.expand(pe.get(0)).andThen(this.expand(pe.get(1)));
	// case Tree:
	// case Fold:
	// return expandTree(pe);
	// /* $(a/b) => $(a)/$(b) */
	// case Link:
	//
	// }
	// }
	//
	// static class Optimized {
	// int spos = 0;
	// int epos = 0;
	// String tag = null;
	// String val = null;
	// }
	//
	// static Expr expandTree(Expr pe) {
	// Optimized opt = new Optimized();
	// int c = countTag(pe.get(0));
	// if (c == 1) {
	// opt.tag = eliminateTag(pe.get(0));
	// }
	// Expr[] list = pe.flattenSeq();
	// int start = list.length;
	// int len = 0;
	// for (int i = 0; i < list.length; i++) {
	// int l = First.fixlen(list[i]);
	// if (l == -1 || !First.unit(list[i])) {
	// start = i;
	// break;
	// }
	// len = len + l;
	// }
	// pe.inner = this.seq(start, list);
	// }
	//
	// static int countTag(Expr pe, HashSet tag) {
	// switch (pe.ctag) {
	// case Tag:
	// tag[0] = (String) pe.param(0);
	// return found + 1;
	// case Seq:
	// }
	// }
	//
	// static Expr expandOr(Expr pe, Function<Expr, Expr> newf) {
	// Expr in = pe.get(0);
	// if (in.ctag == CTag.Or) {
	// return newf.apply(in.get(0)).orElse(expand(newf.apply(in.get(1))));
	// }
	// return pe;
	// }

	static Expr inline(Expr pe) {
		if (pe.ctag == CTag.NonTerm) {
			Expr deref = pe.get(0);
			if (deref.isNonTerm()) {
				return inline(deref);
			}
			if (deref.isChar()) {
				System.err.printf("inline %s refc %s\n", pe.param(0), deref);
				return deref;
			}
			if (deref.isStr()) {
				System.err.printf("inline %s refc %s\n", pe.param(0), deref);
				return deref;
			}
			if (deref.get() instanceof Integer) {
				System.out.printf("%s refc %s", deref.param(0), deref.get());
				// return deref;
			}
			return pe;
		}
		return PEG.dup(pe, ParserGen::inline);
	}

	void makeDict2(String curName, Expr pe, HashMap<String, NonTerm2> nameMap) {
		if (pe != null) {
			if (pe instanceof NonTerm2) {
				String key = this.uname(pe);
				if (!nameMap.containsKey(key)) {
					nameMap.put(key, (NonTerm2) pe);
					this.makeDict2(key, pe.get(0), nameMap);
				}
				return;
			}
			this.makeDict2(curName, pe.get(0), nameMap);
			this.makeDict2(curName, pe.get(1), nameMap);
		}
	}

	// Toplogical Sort

	void deps(String curName, Expr pe, HashMap<String, HashSet<String>> depsMap) {
		if (pe instanceof NonTerm2) {
			String key = this.uname(pe);
			HashSet<String> set = depsMap.get(curName);
			if (set == null) {
				set = new HashSet<>();
				depsMap.put(curName, set);
			}
			set.add(key);
			return;
		}
		if (pe != null) {
			this.deps(curName, pe.get(0), depsMap);
			this.deps(curName, pe.get(1), depsMap);
		}
	}

	ArrayList<String> sortList(HashMap<String, NonTerm2> nameMap, HashSet<String> crossRefs) {
		HashMap<String, HashSet<String>> depsMap = new HashMap<>();
		nameMap.forEach((name, nt) -> {
			this.deps(name, nt.get(0), depsMap);
		});
		System.out.println("deps: " + depsMap);

		class TopologicalSorter {
			private final HashMap<String, HashSet<String>> nodes;
			private final LinkedList<String> result;
			private final HashMap<String, Short> visited;
			private final Short Visiting = 1;
			private final Short Visited = 2;
			private HashSet<String> crossRefNames;

			TopologicalSorter(HashMap<String, HashSet<String>> nodes, HashSet<String> crossRefs) {
				this.nodes = nodes;
				this.result = new LinkedList<>();
				this.visited = new HashMap<>();
				this.crossRefNames = crossRefs;
				for (Map.Entry<String, HashSet<String>> e : this.nodes.entrySet()) {
					if (this.visited.get(e.getKey()) == null) {
						this.visit(e.getKey(), e.getValue());
					}
				}
			}

			private void visit(String key, HashSet<String> nextNodes) {
				this.visited.put(key, this.Visiting);
				if (nextNodes != null) {
					for (String nextNode : nextNodes) {
						Short v = this.visited.get(nextNode);
						if (v == null) {
							this.visit(nextNode, this.nodes.get(nextNode));
						} else if (v == this.Visiting) {
							if (!key.equals(nextNode)) {
								// System.out.println("Cyclic " + key + " => " +
								// nextNode);
								this.crossRefNames.add(nextNode);
							}
						}
					}
				}
				this.visited.put(key, this.Visited);
				this.result.add(key);
			}

			public ArrayList<String> getResult() {
				return new ArrayList<>(this.result);
			}
		}
		TopologicalSorter sorter = new TopologicalSorter(depsMap, crossRefs);
		ArrayList<String> funcList = sorter.getResult();
		return funcList;
	}

	/* Generator */

	static class GeneratorContext {
		HashMap<String, ParserFunc> funcMap = new HashMap<>();
		HashMap<String, Integer> recurMap = new HashMap<>();
		ParserFunc[] base = null;
		HashMap<String, Integer> stateMap = new HashMap<>();
		final boolean checkEOF;

		GeneratorContext(boolean checkEOF, int size) {
			this.checkEOF = checkEOF;
			this.base = new ParserFunc[size];
		}

		int stateId(Object label) {
			Integer n = this.stateMap.get(label.toString());
			if (n == null) {
				n = this.stateMap.size() + 1;
				this.stateMap.put(label.toString(), n);
			}
			return n;
		}
	}

	private ParserFunc cRef(String name, GeneratorContext gx) {
		ParserFunc f = gx.funcMap.get(name);
		if (f != null) {
			return f;
		}
		Integer rec = gx.recurMap.get(name);
		// System.err.println("name " + name + ", rec " + rec);
		if (rec == null) {
			rec = gx.recurMap.size();
			gx.recurMap.put(name, rec);
		}
		return cRef(gx.base, rec);
	}

	ParserFunc gen(Expr pe, GeneratorContext gx) {
		return this.memo(pe, this.gen2(pe, gx));
	}

	ParserFunc gen2(Expr pe, GeneratorContext gx) {
		switch (pe.ctag) {
		case Empty:
			return ParserGen::succ;
		case Char:
			return this.cChar((BitChar) pe.param(0));
		case NonTerm:
			return this.cRef((String) pe.param(0), gx);
		case Seq:
			return this.sequence(pe.flatten(), gx);
		case Or:
			return choice(this.acc(pe), this.gen(pe.flatten(), gx));
		case Alt:
			return choice(this.acc(pe), this.gen(pe.flatten(), gx)); // FIXME
		case And:
			return this.cAnd(pe.get(0), gx);
		case Not:
			return this.cNot(pe.get(0), gx);
		case Many:
			return this.cMany(pe.get(0), gx);
		case OneMore:
			return cAdd2(this.gen(pe.get(0), gx), this.cMany(pe.get(0), gx));
		/* */
		case Tree:
			return this.cTree(pe, gx);
		case Link:
			return this.cLink(pe, gx);
		case Fold:
			return this.cFold(pe, gx);
		case Tag:
			return cTag((String) pe.param(0));
		case Val:
			return ParserGen::succ;
		/* */
		case Scope: /* @symbol(A) */
			return cScope(this.gen(pe.get(0), gx));
		case Symbol: /* @symbol(A) */
			return cSymbol(this.gen(pe.get(0), gx), gx.stateId(pe.param(0)));
		case Exists:
			return cExists(gx.stateId(pe.param(0)));
		case Match:
			return cMatch(gx.stateId(pe.param(0)));
		case Contains:
		case Equals:
		case Eval:
			/* */
		case If: /* @if(flag, e) */
		case On: /* @on(flag, e) */
		case Off: /* @off(flag, e) */
		default:
			return ParserGen::succ;
		}
	}

	ParserFunc[] gen(Expr[] pe, final GeneratorContext gx) {
		return Arrays.stream(pe).map(e -> this.gen(e, gx)).toArray(ParserFunc[]::new);
	}

	private ParserFunc cChar(BitChar bs) {
		int b = bs.single();
		if (b == -1) {
			return cCharInc((byte) b);
		}
		if (!this.isBinary) {
			bs = bs.textVersion();
		}
		return cCharSetInc(bs.bits());
	}

	private ParserFunc cTree(Expr pe, GeneratorContext gx) {
		return cTree(this.gen(pe.get(0), gx), 0, TreeNode.EmptyTag, 0);
	}

	private ParserFunc cLink(Expr pe, GeneratorContext gx) {
		return cLink(this.gen(pe.get(0), gx), (String) pe.param(0));
	}

	private ParserFunc cFold(Expr pe, GeneratorContext gx) {
		return cFold(this.gen(pe.get(0), gx), (String) pe.param(0), 0, TreeNode.EmptyTag, 0);
	}

	private ParserFunc cAnd(Expr pe, GeneratorContext gx) {
		return cAnd(this.gen(pe, gx));
	}

	// generator library

	// Obits32
	private final static boolean bits32(int[] bits, byte b) {
		int n = b & 0xff;
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

	private final static boolean mback7(ParserContext px, int pos, T tree, State state) {
		px.pos = mbackpos(px, pos);
		px.tree = tree;
		px.state = state;
		return true;
	}

	static ParserFunc cRef(final ParserFunc[] base, int index) {
		assert (base != null);
		return (px) -> {
			return base[index].apply(px);
		};
	}

	static boolean succ(ParserContext px) {
		return true;
	}

	static boolean fail(ParserContext px) {
		return false;
	}

	static ParserFunc cChar(final byte b) {
		return (px) -> {
			return px.inputs[px.pos] == b;
		};
	}

	static ParserFunc cCharInc(final byte b) {
		if (b == 0) {
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos++] == b;
			};
		}
		return (px) -> {
			return px.inputs[px.pos++] == b;
		};
	}

	static ParserFunc cChar(final byte b, final byte b2) {
		if (b == 0 || b2 == 0) {
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2;
			};
		}
		return (px) -> {
			return px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2;
		};
	}

	static ParserFunc cChar(final byte b, final byte b2, final byte b3) {
		if (b == 0 || b2 == 0 || b3 == 0) {
			return (px) -> {
				return px.pos < px.length && px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2
						&& px.inputs[px.pos + 2] == b3;
			};
		}
		return (px) -> {
			return px.inputs[px.pos] == b && px.inputs[px.pos + 1] == b2 && px.inputs[px.pos + 2] == b3;
		};
	}

	static ParserFunc cChar(final byte b, final byte b2, final byte b3, final byte b4) {
		if (b == 0 || b2 == 0 || b3 == 0 || b4 == 0) {
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

	static ParserFunc cChar(final byte[] b) {
		final int len = b.length;
		switch (len) {
		case 0:
			return ParserGen::succ;
		case 1:
			return cChar(b[0]);
		case 2:
			return cChar(b[0], b[1]);
		case 3:
			return cChar(b[0], b[1], b[2]);
		case 4:
			return cChar(b[0], b[1], b[2], b[3]);
		default:
			return (px) -> {
				return px.pos + len < px.length && matchmany(px.inputs, px.pos, b, 0, len);
			};
		}
	}

	static ParserFunc cMov(final ParserFunc f, final int len) {
		return (px) -> {
			if (f.apply(px)) {
				px.pos += len;
				return true;
			}
			return false;
		};
	}

	static ParserFunc cCharInc(final byte[] b) {
		return cMov(cChar(b), b.length);
	}

	static ParserFunc cCharSet(final int[] bs) {
		if (bits32(bs, (byte) 0)) {
			return (px) -> {
				return px.pos < px.length && bits32(bs, px.inputs[px.pos]);
			};
		}
		return (px) -> {
			return bits32(bs, px.inputs[px.pos]);
		};
	}

	static ParserFunc cCharSetInc(final int[] bs) {
		if (bits32(bs, (byte) 0)) {
			return (px) -> {
				return px.pos < px.length && bits32(bs, px.inputs[px.pos++]);
			};
		}
		return (px) -> {
			// System.err.println("FIXME" + px.pos + " < " + px.inputs.length);
			return bits32(bs, px.inputs[px.pos++]);
		};
	}

	static ParserFunc cAdd2(ParserFunc f, ParserFunc f2) {
		return (px) -> {
			return f.apply(px) && f2.apply(px);
		};
	}

	static ParserFunc cAdd3(ParserFunc f, ParserFunc f2, ParserFunc f3) {
		return (px) -> {
			return f.apply(px) && f2.apply(px) && f3.apply(px);
		};
	}

	static ParserFunc cAdd4(ParserFunc f, ParserFunc f2, ParserFunc f3, ParserFunc f4) {
		return (px) -> {
			return f.apply(px) && f2.apply(px) && f3.apply(px) && f4.apply(px);
		};
	}

	static ParserFunc cAdd(final ParserFunc... fs) {
		switch (fs.length) {
		case 0:
			return ParserGen::succ;
		case 1:
			return fs[0];
		case 2:
			return cAdd2(fs[0], fs[1]);
		case 3:
			return cAdd3(fs[0], fs[1], fs[2]);
		case 4:
			return cAdd4(fs[0], fs[1], fs[2], fs[3]);
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

	ParserFunc sequence(Expr[] es, GeneratorContext gx) {
		byte[] b = PEG.getstr2(es);
		if (b.length > 1) {
			ParserFunc f = cCharInc(b);
			if (b.length == es.length) {
				return f;
			}
			ParserFunc[] fs = new ParserFunc[es.length - b.length + 1];
			fs[0] = f;
			for (int i = 0; i < fs.length - 1; i++) {
				fs[i + 1] = this.gen(es[b.length + i], gx);
			}
			return cAdd(fs);
		}
		return cAdd(this.gen(es, gx));
	}

	static boolean foundZero(final byte[] b) {
		for (int i = 0; i < b.length; i++) {
			if (b[i] == 0) {
				return true;
			}
		}
		return false;
	}

	HashMap<String, ParserFunc> cacheMap = new HashMap<>();

	ParserFunc memo(Expr pe, ParserFunc f) {
		if (!pe.isNonTerm()) {
			String key = pe.toString();
			ParserFunc f2 = this.cacheMap.get(key);
			if (f2 == null) {
				this.cacheMap.put(key, f);
				f2 = f;
			}
			return f2;
		}
		return f;
	}

	private int[] bits(BitChar bc) {
		return this.isBinary ? bc.bits() : bc.textVersion().bits();
	}

	static ParserFunc cOr2(int acc, ParserFunc f, ParserFunc f2) {
		switch (acc) {
		case 1:
			return (px) -> {
				int pos = px.pos;
				return f.apply(px) || mback1(px, pos) && f2.apply(px);
			};
		case 3:
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				return f.apply(px) || mback3(px, pos, tree) && f2.apply(px);
			};
		default:
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				State state = px.state;
				return f.apply(px) || mback7(px, pos, tree, state) && f2.apply(px);
			};
		}
	}

	static ParserFunc choice(int acc, ParserFunc... fs) {
		switch (fs.length) {
		case 0:
			return ParserGen::fail;
		case 1:
			return fs[0];
		case 2:
			return cOr2(acc, fs[0], fs[1]);
		}
		final int tail = fs.length - 1;
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

	static ParserFunc cDispatch(ParserFunc[] jumptbl) {
		return (px) -> {
			return jumptbl[px.inputs[px.pos] & 0xff].apply(px);
		};
	}

	static ParserFunc cDispatchInc(ParserFunc[] jumptbl) {
		return (px) -> {
			return jumptbl[px.inputs[px.pos++] & 0xff].apply(px);
		};
	}

	/* Many */

	static ParserFunc cMany(int acc, ParserFunc f) {
		switch (acc) {
		case 1:
			return (px) -> {
				int pos = px.pos;
				while (f.apply(px)) {
					pos = px.pos;
				}
				return mback1(px, pos);
			};
		case 3:
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				while (f.apply(px)) {
					pos = px.pos;
					tree = px.tree;
				}
				return mback3(px, pos, tree);
			};
		default:
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
	}

	static ParserFunc cManyX(int acc, ParserFunc f) {
		switch (acc) {
		case 1:
			return (px) -> {
				int pos = px.pos;
				while (f.apply(px) && pos < px.pos) {
					pos = px.pos;
				}
				return mback1(px, pos);
			};
		case 3:
			return (px) -> {
				int pos = px.pos;
				T tree = px.tree;
				while (f.apply(px) && pos < px.pos) {
					pos = px.pos;
					tree = px.tree;
				}
				return mback3(px, pos, tree);
			};
		default:
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
	}

	final static ParserFunc fManyEOF = (px) -> {
		if (px.pos < px.length) {
			px.pos = px.length;
		}
		return true;
	};

	static ParserFunc cMany(boolean checkEOF, final byte b) {
		if (checkEOF || b == 0) {
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

	static ParserFunc cMany(boolean checkEOF, final int[] bm) {
		if (checkEOF || bits32(bm, (byte) 0)) {
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

	static ParserFunc cMany(boolean checkEOF, final byte[] b) {
		if (checkEOF || foundZero(b)) {
			final int len = b.length;
			return (px) -> {
				while (px.pos + len < px.length && matchmany(px.inputs, px.pos, b, 0, len)) {
					px.pos = px.pos + len;
				}
				return true;
			};
		} else {
			final int len = b.length;
			return (px) -> {
				while (matchmany(px.inputs, px.pos, b, 0, len)) {
					px.pos = px.pos + len;
				}
				return true;
			};
		}
	}

	private ParserFunc cMany(Expr pe, GeneratorContext gx) {
		Expr inner = pe.deref();
		if (inner.isChar()) {
			BitChar bc = (BitChar) pe.param(0);
			if (bc.isSingle()) {
				return cMany(gx.checkEOF, bc.single());
			}
			if (bc.isAny()) {
				return fManyEOF;
			}
			return cMany(gx.checkEOF, this.bits(bc));
		}
		if (inner.ctag == CTag.Seq) {
			Expr[] es = inner.flatten();
			byte[] b = PEG.getstr2(es);
			if (es.length == b.length) {
				return cMany(gx.checkEOF, b);
			}
		}
		if (pe.isNullable()) {
			return cManyX(this.acc(pe), this.gen(pe, gx));
		}
		return cMany(this.acc(pe), this.gen(pe, gx));
	}

	static ParserFunc cAnd(ParserFunc f) {
		return (px) -> {
			int pos = px.pos;
			return f.apply(px) || mback1(px, pos);
		};
	}

	/* Not */

	static ParserFunc cNot(int acc, ParserFunc f) {
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

	final static ParserFunc fNotAny = (px) -> {
		return px.pos >= px.length;
	};

	static ParserFunc cNot(boolean checkEOF, final byte b) {
		if (checkEOF || b == 0) {
			return (px) -> {
				return !(px.pos < px.length && px.inputs[px.pos] == b);
			};
		}
		return (px) -> {
			return !(px.inputs[px.pos] == b);
		};
	}

	static ParserFunc cNot(boolean checkEOF, final int[] bm) {
		if (checkEOF || bits32(bm, (byte) 0)) {
			return (px) -> {
				return !(px.pos < px.length && bits32(bm, px.inputs[px.pos]));
			};
		}
		return (px) -> {
			return !bits32(bm, px.inputs[px.pos]);
		};
	}

	static ParserFunc cNot2(boolean checkEOF, final byte b0, final byte b1) {
		if (checkEOF || b0 == 0 || b1 == 0) {
			return (px) -> {
				return !(px.pos + 1 < px.length && px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1);
			};
		} else {
			return (px) -> {
				return !(px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1);
			};
		}
	}

	static ParserFunc cNot3(boolean checkEOF, final byte b0, final byte b1, final byte b2) {
		if (checkEOF || b0 == 0 || b1 == 0 || b2 == 0) {
			return (px) -> {
				return !(px.pos + 1 < px.length && px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1
						&& px.inputs[px.pos + 2] == b2);
			};
		} else {
			return (px) -> {
				return !(px.inputs[px.pos] == b0 && px.inputs[px.pos + 1] == b1 && px.inputs[px.pos + 2] == b2);
			};
		}
	}

	static ParserFunc cNot(boolean checkEOF, final byte[] b) {
		switch (b.length) {
		case 1:
			return cNot(checkEOF, b[0]);
		case 2:
			return cNot2(checkEOF, b[0], b[1]);
		case 3:
			return cNot3(checkEOF, b[0], b[1], b[2]);
		default:
			if (checkEOF || foundZero(b)) {
				final int len = b.length;
				return (px) -> {
					return !(px.pos + len < px.length && matchmany(px.inputs, px.pos, b, 0, len));
				};
			} else {
				final int len = b.length;
				return (px) -> {
					return !matchmany(px.inputs, px.pos, b, 0, len);
				};
			}
		}
	}

	private ParserFunc cNot(Expr pe, GeneratorContext gx) {
		Expr inner = pe.deref();
		if (inner.isChar()) {
			BitChar bc = (BitChar) pe.param(0);
			if (bc.isSingle()) {
				return cNot(gx.checkEOF, bc.single());
			}
			if (bc.isAny()) {
				return fNotAny;
			}
			return cNot(gx.checkEOF, this.bits(bc));
		}
		if (inner.ctag == CTag.Seq) {
			Expr[] es = inner.flatten();
			byte[] b = PEG.getstr2(es);
			if (es.length == b.length) {
				return cNot(gx.checkEOF, b);
			}
		}
		return cNot(this.acc(pe), this.gen(pe, gx));
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

	static ParserFunc cTree(final ParserFunc f, final int spos, final String tag, final int epos) {
		return (px) -> {
			int pos = px.pos;
			px.tree = null;
			return f.apply(px) && mtree(px, tag, pos + spos, px.pos + epos);
		};
	}

	static ParserFunc cLink(ParserFunc f, String tag) {
		return (px) -> {
			T tree = px.tree;
			return f.apply(px) && mlink(px, tag, px.tree, tree);
		};
	}

	static ParserFunc cFold(final ParserFunc f, final String label, final int spos, final String tag, final int epos) {
		return (px) -> {
			int pos = px.pos;
			return mlink(px, label, px.tree, null) && f.apply(px) && mtree(px, tag, pos + spos, px.pos + epos);
		};
	}

	static ParserFunc cTag(final String tag) {
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

	private final static boolean mconsume7(ParserContext px, MemoEntry memo) {
		px.pos = memo.mpos;
		px.tree = memo.mtree;
		px.state = memo.mstate;
		return memo.matched;
	}

	private final static boolean mstore7(ParserContext px, MemoEntry memo, long key, int pos, boolean matched) {
		memo.key = key;
		memo.mpos = pos;
		memo.mtree = px.tree;
		memo.mstate = px.state;
		memo.matched = matched;
		return matched;
	}

	ParserFunc cMemo(ParserFunc f, int mp) {
		return (px) -> {
			int pos = px.pos;
			long key = getkey(pos, mp);
			MemoEntry memo = getmemo(px, key);
			return (memo.key == key) ? mconsume7(px, memo) : mstore7(px, memo, key, pos, f.apply(px));
		};
	}

	private final static boolean mstate(ParserContext px, int ns, int pos) {
		px.state = new State(ns, pos, px.pos, px.state);
		return true;
	}

	static ParserFunc cScope(ParserFunc pe) {
		return (px) -> {
			State state = px.state;
			return pe.apply(px) && mback4(px, state);
		};
	}

	static ParserFunc cSymbol(ParserFunc pe, int ns) {
		return (px) -> {
			int pos = px.pos;
			return pe.apply(px) && mstate(px, ns, pos);
		};
	}

	final static State getstate(State state, int ns) {
		return (state == null || state.ns == ns) ? state : getstate(state.sprev, ns);
	}

	static ParserFunc cMatch(int ns) {
		return (px) -> {
			State state = getstate(px.state, ns);
			return state != null && matchmany(px.inputs, px.pos, px.inputs, state.spos, state.slen)
					&& mmov(px, state.slen);
		};
	}

	static ParserFunc cExists(int ns) {
		return (px) -> getstate(px.state, ns) != null;
	}

}