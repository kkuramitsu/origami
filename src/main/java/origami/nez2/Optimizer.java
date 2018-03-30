package origami.nez2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

import origami.nez2.Expr.PTag;
import origami.nez2.PEG.Memoed;
import origami.nez2.PEG.NonTerm;
import origami.nez2.TPEG.OptimizedTree;

class Optimizer {

	static boolean isVerbose = false;
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
			// System.err.printf("ast %s = %s%n", name, nt.get(0));
			nt.inner = trace("ast", name, nt.get(0), (p) -> TPEG.checkAST(p));
		});
		// 1. optimizing ..
		nameMap.forEach((name, nt) -> {
			// nt.inner = trace("dfa", name, nt.get(0), (p) -> DFA.optimizeChoice(p));
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
		// countMap.forEach((name, n) -> {
		// gx.log("refc ... " + name + " = " + n);
		// });

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
		if (isVerbose && !pe.toString().equals(pe2.toString())) {
			System.out.printf("optimized %s %s\n\t%s\n\t=> %s\n", p, name, pe, pe2);
		}
		return pe2;
	}

	void makeDict(String curName, Expr pe, HashMap<String, Expr> prodMap, TreeSet<String> flagSet) {
		if (pe != null) {
			switch (pe.ptag) {
			case NonTerm:
				Expr deref = pe.get(0);
				if (deref != null) {
					String key = this.uname((NonTerm) pe);
					if (!prodMap.containsKey(key)) {
						prodMap.put(key, deref);
						this.makeDict(key, deref, prodMap, flagSet);
					}
				}
				return;
			case Char:
				if (pe.bitChar().isBinary()) {
					this.isBinary = true;
				}
				return;
			case If:
				flagSet.add(pe.label());
				return;
			default:
				this.makeDict(curName, pe.get(0), prodMap, flagSet);
				this.makeDict(curName, pe.get(1), prodMap, flagSet);
			}
		}
	}

	private final String uname(NonTerm pe) {
		return pe.uname();
	}

	Expr rename(Expr pe, HashMap<String, Expr> prodMap, HashMap<String, NonTerm2> nameMap, Flags flags) {
		switch (pe.ptag) {
		case NonTerm: {
			Expr inner = pe.get(0);
			assert !(pe instanceof NonTerm2);
			if (inner != null) {
				String key = this.uname((NonTerm) pe);
				String uname = flags.uname(key, (Memoed) pe);
				NonTerm2 nt = nameMap.get(uname);
				if (nt == null) {
					nt = new NonTerm2(uname, pe.params(), PEG.Empty_);
					nameMap.put(uname, nt);
					nt.inner = this.rename(inner, prodMap, nameMap, flags);
				}
				return nt;
			}
			return new Var(pe.label(), ((NonTerm) pe).index);
		}
		case If:
			if (flags.is(pe.label())) {
				return PEG.Empty_;
			}
			return PEG.Fail_;
		case On: {
			boolean stacked = flags.is(pe.label());
			flags.set(pe.label(), true);
			Expr inner = this.rename(pe.get(0), prodMap, nameMap, flags);
			flags.set(pe.label(), stacked);
			return inner;
		}
		case Off: {
			boolean stacked = flags.is(pe.label());
			flags.set(pe.label(), false);
			Expr inner = this.rename(pe.get(0), prodMap, nameMap, flags);
			flags.set(pe.label(), stacked);
			return inner;
		}
		case OneMore: {
			Expr inner = this.rename(pe.get(0), prodMap, nameMap, flags);
			return inner.andThen(new PEG.Many(inner));
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
			checkLeftRecur(nt.label(), nt.get(0));
		} catch (Exception e) {
			nt.peg.log("left recursion %s", nt);
		}
	}

	static boolean checkLeftRecur(String name, Expr pe) {
		switch (pe.ptag) {
		case NonTerm:
			if (name.equals(pe.label())) {
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
			Hack.TODO(pe);
			return true;
		}
	}

	// Elimination

	// Tree

	static Expr optTree(Expr pe) {
		switch (pe.ptag) {
		case Tree:
		case Fold: {
			OptimizedTree t = (OptimizedTree) pe;
			return t.optimize();
		}
		default:
			return PEG.dup(pe, Optimizer::optTree);
		}

	}

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
			String key = pe.label();
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
			String key = pe.label();
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