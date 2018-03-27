package nez2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

import nez2.PEG.Expr;
import nez2.PEG.ExprP1;
import nez2.PEG.Memoed;
import nez2.PEG.NonTerm;
import nez2.PEG.PTag;
import nez2.PEG.Val;

class Optimizer {

	boolean isBinary = false;
	HashMap<String, Object> memoed = new HashMap<>();

	<X> X generate(String start, Expr pe, Generator<X> gx) {
		HashMap<String, Expr> prodMap = new HashMap<>();
		prodMap.put(start, pe);
		TreeSet<String> flagSet = new TreeSet<>();
		this.makeDict(start, pe, prodMap, flagSet);
		gx.log("start: " + start + " prodMap: " + prodMap);
		if (flagSet.size() > 0) {
			gx.log("flagSet: " + flagSet);
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
		nameMap.forEach((name, nt) -> {
			nt.inner = trace("dfa", name, nt.get(0), (p) -> DFA.optimizeChoice(p));
		});
		nameMap.forEach((name, nt) -> {
			nt.inner = trace("inline", name, nt.get(0), (p) -> inline(p));
		});

		// 2. generating ..
		HashMap<String, Expr> nameMap2 = new HashMap<>();
		nameMap2.put(start, snt.get(0));
		HashMap<String, Integer> countMap = new HashMap<>();
		this.makeDict2(start, snt.get(0), nameMap2, countMap);
		gx.log("size: " + nameMap.size() + " => " + nameMap2.size());
		countMap.forEach((name, n) -> {
			gx.log("refc ... " + name + " = " + n);
		});

		HashSet<String> crossRefs = new HashSet<>();
		List<String> list = this.sortList(nameMap2, crossRefs);
		if (!list.contains(start)) {
			list.add(start);
		}
		gx.log("list: " + list);
		gx.log("crossrefs: " + crossRefs);
		return gx.generate(start, nameMap2, list);
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
			switch (pe.ptag) {
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
		switch (pe.ptag) {
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
		case OneMore: {
			return pe.get(0).andThen(new PEG.Many(pe.get(0)));
		}
		case Tree: {
			Tree2 t = new Tree2(PEG.Empty_);
			Expr[] es = this.rename(pe.get(0), prodMap, nameMap, flags).flatten(PTag.Seq);
			Expr pe2 = t.optimize(es, t);
			// System.err.println("@@@ " + pe + " ==> " + pe2);
			return pe2;
		}
		case Fold: {
			Fold2 t = new Fold2(pe.p(0), PEG.Empty_);
			Expr[] es = this.rename(pe.get(0), prodMap, nameMap, flags).flatten(PTag.Seq);
			Expr pe2 = t.optimize(es, t);
			// System.err.println("@@@ " + pe + " ==> " + pe2);
			return pe2;
		}
		default:
			return PEG.dup(pe, (p) -> this.rename(p, prodMap, nameMap, flags));
		}
	}

	class NonTerm2 extends Memoed {
		Expr inner;
		String[] params;

		public NonTerm2(String name, String params[], Expr inner) {
			this.ptag = PTag.NonTerm;
			this.memoed = Optimizer.this.memoed;
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
		switch (pe.ptag) {
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

	// Tree

	static class OptTree extends ExprP1 {
		int spos = 0;
		int epos = 0;
		String tag = null;
		byte[] val = null;

		Expr dupi(OptTree t) {
			t.spos = this.spos;
			t.epos = this.epos;
			t.tag = this.tag;
			t.val = this.val;
			return t;
		}

		Expr optimize(Expr[] es, OptTree t) {
			for (int i = es.length - 1; i >= 0; i--) {
				if (es[i].ptag == PTag.Tag) {
					t.tag = es[i].p(0);
					es[i] = PEG.Empty_;
					break;
				}
				if (!Trees.isUnit(es[i])) {
					break;
				}
			}
			for (int i = es.length - 1; i >= 0; i--) {
				if (es[i].ptag == PTag.Val) {
					t.val = (byte[]) es[i].param(0);
					es[i] = PEG.Empty_;
					break;
				}
				if (!Trees.isUnit(es[i])) {
					break;
				}
			}
			int start = es.length;
			for (int i = 0; i < es.length; i++) {
				int len = First.fixlen(es[i]);
				if (len == -1 || !Trees.isUnit(es[i])) {
					start = i;
					break;
				}
				t.spos -= len;
			}
			t.inner = PEG.seq(start, es.length, es);
			if (start > 0) {
				Expr head = PEG.seq(0, start, es);
				return head.andThen(t);
			}
			return t;
		}

		@Override
		public Expr andThen(Expr next) {
			int len = First.fixlen(next);
			if (len != -1) {
				this.epos = -len;
				this.inner = this.inner.andThen(next);
				return this;
			}
			return super.andThen(next);
		}

		@Override
		public void strOut(StringBuilder sb) {
			PEG.showing(false, this, sb);
			sb.append("[");
			sb.append(this.spos);
			sb.append(",");
			sb.append(this.epos);
			if (this.tag != null) {
				sb.append(",#");
				sb.append(this.tag);
			}
			if (this.val != null) {
				sb.append(",");
				sb.append(new Val(this.val));
			}
			sb.append("]");
		}

	}

	static class Tree2 extends OptTree {

		Tree2(Expr inner) {
			this.ptag = PTag.Tree;
			this.inner = inner;
		}

		@Override
		Expr dup(Object p, Expr... es) {
			return this.dupi(new Tree2(es[0]));
		}

	}

	static class Fold2 extends OptTree {

		Fold2(String label, Expr inner) {
			this.ptag = PTag.Fold;
			this.label = label;
			this.inner = inner;
		}

		@Override
		Expr dup(Object p, Expr... es) {
			return this.dupi(new Fold2(this.label, es[0]));
		}

	}

	static Expr optTree(Expr pe) {
		switch (pe.ptag) {
		case Tree: {
			Tree2 t = new Tree2(PEG.Empty_);
			Expr[] es = pe.get(0).flatten(PTag.Seq);
			return t.optimize(es, t);
		}
		case Fold: {
			Fold2 t = new Fold2(pe.p(0), PEG.Empty_);
			Expr[] es = pe.get(0).flatten(PTag.Seq);
			return t.optimize(es, t);
		}
		default:
			return PEG.dup(pe, Optimizer::optTree);
		}
	}

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
		if (pe.ptag == PTag.NonTerm) {
			Expr deref = pe.get(0);
			if (deref.isNonTerm()) {
				return inline(deref);
			}
			if (deref.isChar()) {
				// System.err.printf("inline %s refc %s\n", pe.param(0), deref);
				return deref;
			}
			if (deref.isStr()) {
				// System.err.printf("inline %s refc %s\n", pe.param(0), deref);
				return deref;
			}
			if (deref.get() instanceof Integer) {
				// System.out.printf("%s refc %s", deref.param(0), deref.get());
				// return deref;
			}
			return pe;
		}
		return PEG.dup(pe, Optimizer::inline);
	}

	void makeDict2(String curName, Expr pe, HashMap<String, Expr> nameMap, HashMap<String, Integer> countMap) {
		if (pe instanceof NonTerm2) {
			String key = this.uname(pe);
			if (!nameMap.containsKey(key)) {
				nameMap.put(key, pe.get(0));
				this.makeDict2(key, pe.get(0), nameMap, countMap);
			}
			Integer n = countMap.get(key);
			n = n == null ? 1 : n + 1;
			countMap.put(key, n);
			return;
		}
		for (int i = 0; i < pe.size(); i++) {
			this.makeDict2(curName, pe.get(i), nameMap, countMap);
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
		for (int i = 0; i < pe.size(); i++) {
			this.deps(curName, pe.get(i), depsMap);
		}
	}

	ArrayList<String> sortList(HashMap<String, Expr> nameMap, HashSet<String> crossRefs) {
		HashMap<String, HashSet<String>> depsMap = new HashMap<>();
		nameMap.forEach((name, e) -> {
			this.deps(name, e, depsMap);
		});
		// System.out.println("deps: " + depsMap);

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
		return sorter.getResult();
	}

}