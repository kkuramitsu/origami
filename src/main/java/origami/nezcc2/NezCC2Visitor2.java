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

package origami.nezcc2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import blue.origami.parser.ParserGrammar.MemoPoint;
import origami.nez2.BitChar;
import origami.nez2.DFA;
import origami.nez2.Expr;
import origami.nez2.First;
import origami.nez2.Generator;
import origami.nez2.Hack;
import origami.nez2.TPEG;
import origami.nez2.TPEG.OptimizedTree;
import origami.nezcc2.NezCC2.ENode;

class NezCC2Visitor2 implements Generator<Void> {
	final int mask;
	final static int POS = NezCC2.POS;
	final static int TREE = NezCC2.TREE;
	final static int STATE = NezCC2.STATE;
	final static int EMPTY = NezCC2.EMPTY;

	private ArrayList<Expr> waitingList = new ArrayList<>();
	final private NezCC2 pg;

	NezCC2Visitor2(NezCC2 pg, int mask) {
		this.pg = pg;
		this.mask = mask;
		if (pg.isDefined("comment")) {
			this.comment = pg.s("comment");
		}
	}

	@Override
	public Void generate(String start, HashMap<String, Expr> nameMap, List<String> list,
			HashMap<String, Integer> memoMap) {

		this.pg.declConst(this.pg.T("pos"), "memosize", "0");
		this.pg.declConst(this.pg.T("pos"), "memolen", "1");
		this.log("memosize: 0");
		this.log("mask: " + this.mask);
		Expr p = nameMap.get(start);
		this.waitingList.add(p.get(0));
		int c = 0;
		for (int i = 0; i < this.waitingList.size(); i++) {
			Expr e0 = this.waitingList.get(i);
			String funcName = this.getFuncName(e0);
			// MemoPoint memoPoint = null;
			if (e0.isNonTerm()) {
				// memoPoint = g.getMemoPoint(e0.label());
				e0 = e0.get(0);
			}
			if (!this.isDefinedSection(funcName)) {
				this.setCurrentFuncName(funcName);
				// pg.writeSection(pg.emitAsm(pg.format("comment", target)));
				this.define(funcName, this.pg.define(funcName, "px").add(this.eMemo(e0, null/* memoPoint */)));
			}
			c++;
		}
		this.log("funcsize: %d", c);
		return null;
	}

	private ENode eMemo(Expr e, MemoPoint m) {
		if (m == null || !this.pg.isDefined("Omemo")) {
			return this.eMatch(e, false);
		}
		int acc = this.mask;
		String funcName = this.funcAcc("memo", acc);
		this.pg.used("getkey");
		this.pg.used("getmemo");
		this.pg.used("mconsume" + acc);
		this.pg.used("mstore" + acc);
		return this.pg.apply(funcName, "px", m.id + 1, this.eLambda(e));
	}

	private ENode eMatch(Expr e, boolean asFunction) {
		if (asFunction) {
			String funcName = this.getFuncName(e);
			this.waitingList.add(e);
			this.addFunctionDependency(this.getCurrentFuncName(), funcName);
			return this.pg.apply(funcName, "px");
		} else {
			return this.visit(e);
		}
	}

	boolean alwaysFunc(Expr e) {
		switch (e.tag()) {
		case DFA:
		case NonTerm:
			return true;
		default:
			return false;
		}
	}

	private ENode eMatch(Expr e) {
		return this.eMatch(e, this.alwaysFunc(e));
	}

	private ENode eSucc() {
		return this.pg.p("true");
	}

	private ENode eFail() {
		return this.pg.p("false");
	}

	private ENode eNext() {
		return this.pg.apply("mnext1", "px");
	}

	private ENode eNotEOF() {
		return this.pg.p("px.pos < px.length");
	}

	private ENode eNotEOFNext() {
		if (this.pg.isDefined("posinc")) {
			return this.pg.p("px.length < posinc!(px)");
		}
		return this.eNotEOF().and(this.eNext());
	}

	private ENode eMatchByte(int uchar) {
		ENode expr = this.pg.p("px.inputs[px.pos] == $0", (char) (uchar & 0xff));
		if (uchar == 0) {
			expr = this.eNotEOF().and(expr);
		}
		return expr;
	}

	private ENode eMatchByteNext(int uchar) {
		if (this.pg.isDefined("posinc")) {
			ENode expr = this.pg.p("px.inputs[posinc!(px)] == $0", (char) (uchar & 0xff));
			if (uchar == 0) {
				expr = this.eNotEOF().and(expr);
			}
			return expr;
		}
		return this.eMatchByte(uchar).and(this.eNext());
	}

	private ENode eMatchBitChar(BitChar bc) {
		if (bc.isSingle()) {
			return this.eMatchByte(bc.single());
		}
		ENode index = this.pg.p("px.inputs[px.pos]");
		if (this.pg.isDefined("Obits32")) {
			return this.pg.apply("bits32", bc, index);
		} else {
			return this.pg.p("$0[unsigned!($1)]", bc, index);
		}
	}

	private ENode eMatchBitCharNext(BitChar bc) {
		if (bc.isSingle()) {
			return this.eMatchByteNext(bc.single());
		}
		if (this.pg.isDefined("posinc")) {
			ENode index = this.pg.p("px.inputs[posinc!(px)]");
			if (this.pg.isDefined("Obits32")) {
				return this.pg.apply("bits32", bc, index);
			} else {
				return this.pg.p("$0[unsigned!($1)]", bc, index);
			}
		}
		return this.eMatchBitChar(bc).and(this.eNext());
	}

	private ENode eMatchBytes(byte[] b) {
		return this.eMatchBytes(b, 0);
	}

	private ENode eMatchBytes(byte[] b, int offset) {
		int len = b.length - offset;
		// System.err.println("offset=" + offset + ",b=" + b.length);
		if (len == 1) {
			return this.eMatchByte(b[0] & 0xff);
		}
		if (len == 2) {
			return this.pg.apply("match2", "px", (char) b[offset], (char) b[offset + 1]);
		}
		if (len == 3) {
			return this.pg.apply("match3", "px", (char) b[offset], (char) b[offset + 1], (char) b[offset + 2]);
		}
		if (len == 4) {
			return this.pg.apply("match4", "px", (char) b[offset], (char) b[offset + 1], (char) b[offset + 2],
					(char) b[offset + 3]);
		}
		return this.pg.apply("matchmany", "px.inputs", "px.pos", this.pg.data(b), 0, b.length);
	}

	private ENode eMatchBytesNext(byte[] b) {
		return this.eMatchBytes(b, 0).and(this.pg.apply("mmov", "px", b.length));
	}

	ENode eChoice(Expr inner, Expr inner2) {
		int acc = this.varStacks(POS, inner);
		ENode lambda = this.eLambda(inner);
		int acc2 = this.varStacks(POS, inner2);
		ENode lambda2 = this.eLambda(inner2);
		String func = this.funcAcc("or", acc | acc2);
		this.pg.used("mback" + (acc | acc2));
		return this.pg.apply(func, "px", lambda, lambda2);
	}

	ENode eChoice3(Expr inner, Expr inner2, Expr inner3) {
		int acc = this.varStacks(POS, inner);
		ENode lambda = this.eLambda(inner);
		int acc2 = this.varStacks(POS, inner2);
		ENode lambda2 = this.eLambda(inner2);
		int acc3 = this.varStacks(POS, inner3);
		ENode lambda3 = this.eLambda(inner3);
		this.pg.used("mback" + (acc | acc2 | acc3));
		String func = this.funcAcc("oror", acc | acc2 | acc3);
		return this.pg.apply(func, "px", lambda, lambda2, lambda3);
	}

	public ENode visit(Expr e) {
		switch (e.tag()) {
		case NonTerm: {
			String funcName = this.getFuncName(e);
			this.waitingList.add(e);
			this.addFunctionDependency(this.getCurrentFuncName(), funcName);
			return this.pg.apply(funcName, "px");
		}
		case Empty:
			return this.eSucc();
		case Char: {
			BitChar bc = e.bitChar();
			if (bc.isAny()) {
				return this.eNotEOF().and(this.eNext());
			}
			return this.eMatchBitCharNext(bc);

		}
		case Seq: {
			// ArrayList<Integer> l = new ArrayList<>();
			// Expr remain = Expr.extractMultiBytes(e, l);
			// if (l.size() > 1) {
			// byte[] text = Expr.toMultiBytes(l);
			// ENode first = this.eMatchBytesNext(text);
			// if (!(remain instanceof PEmpty)) {
			// first = first.and(this.eMatch(remain, this.pg));
			// }
			// return first;
			// }
			ENode pe1 = this.eMatch(e.get(0));
			ENode pe2 = this.eMatch(e.get(1));
			return pe1.and(pe2);
		}
		case Or: {
			// @Override
			// public ENode visitOption(POption e, NezCC2 pg) {
			// return this.eOption(e.get(0), pg);
			// }

			if (e.size() == 2) {
				return this.eChoice(e.get(0), e.get(1));
			} else if (e.size() == 3) {
				return this.eChoice3(e.get(0), e.get(1), e.get(2));
			} else {
				// System.err.println("choice " + e.size());
				int acc = this.varStacks(POS, e);
				ENode main = this.eMatch(e.get(0));
				for (int i = 1; i < e.size(); i++) {
					main = this.pg.p("($0 || $1 && $2)", main, this.eBack(this.pg, acc), this.eMatch(e.get(i)));
				}
				return this.eLetAccIn(acc, main, this.pg);
			}
		}
		case DFA: {
			DFA dfa = (DFA) e;
			boolean hasJumpTable = this.pg.isDefined("Ojumptable");
			List<ENode> exprs = new ArrayList<>(e.size() + 1);
			ENode index = null;
			exprs.add(hasJumpTable ? this.pg.p("^fail") : this.eFail());
			if (dfa.isDFA() && this.pg.isDefined("posinc")) {
				for (int i = 0; i < e.size(); i++) {
					Expr indexed = DFA.cdr(e.get(i));
					exprs.add(hasJumpTable ? this.eLambda(indexed) : this.eMatch(indexed));
					// exprs.add(hasJumpTable ? this.pg.p("^succ") :
					// this.eSucc());
				}
				index = this.eJumpIndex(dfa.charMap(), true);
			} else {
				for (int i = 0; i < e.size(); i++) {
					Expr indexed = DFA.cdr(e.get(i));
					exprs.add(hasJumpTable ? this.eLambda(indexed) : this.eMatch(indexed));
				}
				index = this.eJumpIndex(dfa.charMap(), false);
			}
			return this.pg.dispatch(index, exprs);
		}
		case Many: {
			return this.eMany(false, e.get(0));
		}
		case OneMore: {
			return this.eMany(true, e.get(0));
		}
		case And: {
			return this.eAnd(e.get(0));
		}
		case Not: {
			return this.eNot(e.get(0));
		}
		case Tree: {
			if (this.isTreeConstruction()) {
				ENode inner = this.eLambda(e.get(0));
				this.pg.used("mtree");
				OptimizedTree t = (OptimizedTree) e;
				return this.pg.apply("newtree", "px", t.spos, inner, t.tag, t.epos);
			}
			return this.eMatch(e.get(0));
		}
		case Fold: {
			if (this.isTreeConstruction()) {
				ENode inner = this.eLambda(e.get(0));
				this.pg.used("mtree");
				this.pg.used("mlink");
				OptimizedTree t = (OptimizedTree) e;
				return this.pg.apply("foldtree", "px", t.spos, "\"" + e.label() + "\"", inner, t.tag, t.epos);
			}
			return this.eMatch(e.get(0));
		}
		case Link: {
			if (this.isTreeConstruction()) {
				ENode lambda = this.eLambda(e.get(0));
				this.pg.used("mlink");
				return this.pg.apply("linktree", "px", "\"" + e.label() + "\"", lambda);
			}
			return this.eMatch(e.get(0));
		}
		case Untree: {
			if (this.isTreeConstruction()) {
				ENode lambda = this.eLambda(e.get(0));
				this.pg.used("mback2");
				return this.pg.apply("detree", "px", lambda);
			}
			return this.eMatch(e.get(0));
		}
		case Tag: {
			if (this.isTreeConstruction()) {
				this.pg.used("mlink");
				return this.pg.apply("tagtree", "px", "\"" + e.label() + "\"");
			}
			return this.eSucc();
		}
		case Val: {
			return this.eSucc();
		}
		case Scope: {
			ENode lambda = this.eLambda(e.get(0));
			this.pg.used("mback4");
			return this.pg.apply("scope4", "px", lambda);
		}
		case Symbol: {
			ENode lambda = this.eLambda(e.get(0));
			this.pg.used("mstate");
			return this.pg.apply("symbol4", "px", this.stateId(e.label()), lambda);
		}
		case Match: {
			this.pg.used("matchmany");
			return this.pg.apply("smatch4", "px", this.stateId(e.label()));
		}
		case Exists: {
			this.pg.used("matchmany");
			return this.pg.apply("sexists4", "px", this.stateId(e.label()));
		}
		case Equals: {
			this.pg.used("matchmany");
			return this.pg.apply("sequals4", "px", this.stateId(e.label()), this.eLambda(e.get(0)));
		}
		case Contains: {
			this.pg.used("smany");
			this.pg.used("matchmany");
			return this.pg.apply("scontains4", "px", this.stateId(e.label()), this.eLambda(e.get(0)));
		}
		case If:
		case On:
		case Off:
			return this.eSucc();
		default:
			Hack.TODO(e);
			return this.eSucc();
		}
	}

	private ENode eJumpIndex(byte[] indexMap, boolean inc) {
		ENode index = inc ? this.pg.p("posinc!(px)") : this.pg.p("px.pos");
		boolean hasMinusIndex = false;
		for (byte b : indexMap) {
			if (b < 0) {
				hasMinusIndex = true;
				break;
			}
		}
		if (hasMinusIndex) {
			return this.pg.p("unsigned!($0[unsigned!(px.inputs[$1])])", indexMap, index);
		}
		return this.pg.p("$0[unsigned!(px.inputs[$1])]", indexMap, index);
	}

	byte[] bytes(Expr p) {
		return null;
	}

	ENode eOption(Expr inner) {
		Expr p = inner.deref();
		BitChar bs = p.bitChar();
		if (bs != null) {
			if (bs.isAny()) {
				return this.eNotEOFNext();
			}
			return this.eMatchBitCharNext(bs);
		}
		byte[] text = this.bytes(p);
		if (text != null) {
			return this.eMatchBytesNext(text).or(this.eSucc());
		}
		int acc = this.varStacks(POS, inner);
		ENode lambda = this.eLambda(inner);
		String func = this.funcAcc("maybe", acc);
		this.pg.used("mback" + acc);
		return this.pg.apply(func, "px", lambda);
	}

	private ENode eMany(Expr inner) {
		Expr p = inner.deref();
		BitChar bs = p.bitChar();
		if (bs != null) {
			if (p.isAny()) {
				return this.pg.apply("manyany", "px");
			}
			return this.pg.apply("manychar", "px", bs);
		}
		byte[] text = this.bytes(p);
		if (text != null) {
			// System.err.println("lex: *" + inner);
			return this.pg.apply("manystr", "px", this.pg.data(text), text.length);
		}
		ENode lambda = this.eLambda(inner);
		int acc = this.varStacks(POS, inner);
		this.pg.used("mback" + acc);
		if (!First.nonnull(inner)) {
			acc |= EMPTY;
		}
		String func = this.funcAcc("many", acc);
		return this.pg.apply(func, "px", lambda);
	}

	private ENode eMany(boolean isOneMore, Expr inner) {
		return (isOneMore) ? this.eMatch(inner).and(this.eMany(inner)) : this.eMany(inner);
	}

	private ENode eAnd(Expr inner) {
		Expr p = inner.deref();
		BitChar bs = p.bitChar();
		if (bs != null) {
			if (bs.isAny()) {
				return this.eNotEOF();
			}
			return this.eMatchBitChar(bs);
		}
		byte[] text = this.bytes(p);
		if (text != null) {
			return this.eMatchBytes(text);
		}
		ENode lambda = this.eLambda(inner);
		this.pg.used("mback1");
		return this.pg.apply("and1", "px", lambda);
	}

	private ENode eNot(Expr inner) {
		Expr p = inner.deref();
		if (p.isEmpty()) {
			return this.eFail();
		}
		BitChar bs = p.bitChar();
		if (bs != null) {
			if (bs.isAny()) {
				return this.pg.p("!($0)", this.eNotEOF());
			}
			return this.pg.p("!($0)", this.eMatchBitChar(bs));
		}
		byte[] text = this.bytes(p);
		if (text != null) {
			// System.err.println("lex: !" + inner);
			return this.pg.p("!($0)", this.eMatchBytes(text));
		}
		// System.err.println("lex: !" + inner);
		int acc = this.varStacks(POS, inner);
		ENode lambda = this.eLambda(inner);
		String func = this.funcAcc("not", acc);
		this.pg.used("mback" + acc);
		return this.pg.apply(func, "px", lambda);
	}

	private ENode eLambda(Expr inner) {
		if (this.pg.isDefined("lambda")) {
			if (this.alwaysFunc(inner) && this.pg.isDefined("funcref")) {
				this.eMatch(inner, true);
				return this.pg.p("^" + this.getFuncName(inner));
			}
			ENode e = this.eMatch(inner, false);
			return this.pg.apply(null, "px", e);
		} else {
			this.eMatch(inner, true);
			return this.pg.p("^" + this.getFuncName(inner));
		}
	}

	private boolean isTreeConstruction() {
		return (this.mask & TREE) == TREE;
	}

	private HashMap<String, Integer> stateIdMap = new HashMap<>();

	int stateId(String id) {
		Integer n = this.stateIdMap.get(id);
		if (n == null) {
			n = this.stateIdMap.size() + 1;
			this.stateIdMap.put(id, n);
		}
		return n;
	}

	protected int varStacks(int acc, Expr e) {
		if (!TPEG.isUnit(e)) {
			acc |= TREE;
		}
		// if (Stateful.isStateful(e)) {
		// acc |= (STATE | TREE);
		// }
		return acc & this.mask;
	}

	private String funcAcc(String f, int acc) {
		return f + acc;
	}

	private ENode eLetAccIn(int acc, ENode e, NezCC2 pg) {
		if ((acc & STATE) == STATE) {
			e = pg.p("let state = px.state; $0", e);
		}
		if ((acc & TREE) == TREE) {
			e = pg.p("let tree = px.tree; $0", e);
		}
		if ((acc & POS) == POS) {
			e = pg.p("let pos = px.pos; $0", e);
		}
		return e;
	}

	private Object[] accNames(int acc, String... prefixes) {
		ArrayList<String> l = new ArrayList<>();
		for (String s : prefixes) {
			l.add(s);
		}
		if ((acc & POS) == POS) {
			l.add("pos");
		}
		if ((acc & TREE) == TREE) {
			l.add("tree");
		}
		if ((acc & STATE) == STATE) {
			l.add("state");
		}
		return l.toArray(new Object[l.size()]);
	}

	private ENode eBack(NezCC2 pg, int acc) {
		String funcName = "mback" + (acc & ~(EMPTY));
		Object[] args = this.accNames(acc & ~(EMPTY), "px");
		return pg.apply(funcName, args);
	}

	// function
	String comment = "";
	HashMap<String, String> exprFuncMap = new HashMap<>();
	HashMap<String, String> termMap = new HashMap<>();

	protected String getFuncName(Expr e) {
		String suffix = "";
		if (e.isNonTerm()) {
			String uname = e.label(); // uniquename
			suffix = String.format(this.comment, uname);
			// MemoPoint memoPoint = this.g.getMemoPoint(uname);
			// if (memoPoint != null) {
			// String fname = this.exprFuncMap.get(uname);
			// if (fname == null) {
			// fname = "e" + this.exprFuncMap.size() + suffix;
			// this.exprFuncMap.put(uname, fname);
			// }
			// return fname;
			// }
			e = e.get(0);
		}
		String key = e.toString();
		String name = this.exprFuncMap.get(key);
		if (name == null) {
			name = "e" + this.exprFuncMap.size() + suffix;
			this.exprFuncMap.put(key, name);
		}
		return name;
	}

	protected HashMap<String, String> funcMap = new HashMap<>();

	boolean isDefinedSection(String funcName) {
		return this.funcMap.containsKey(funcName);
	}

	void define(String funcName, ENode funcBody) {
		this.funcMap.put(funcName, funcBody.toString());
	}

	String getParseFunc(String funcName) {
		return this.funcMap.get(funcName);
	}

	private String currentFuncName = null;

	void setCurrentFuncName(String funcName) {
		this.currentFuncName = funcName;
	}

	String getCurrentFuncName() {
		return this.currentFuncName;
	}

	HashSet<String> crossRefNames = new HashSet<>();
	HashMap<String, HashSet<String>> depsMap = new HashMap<>();

	protected final void addFunctionDependency(String sour, String dest) {
		if (sour != null) {
			HashSet<String> set = this.depsMap.get(sour);
			if (set == null) {
				set = new HashSet<>();
				this.depsMap.put(sour, set);
			}
			set.add(dest);
		}
	}

	ArrayList<String> sortFuncList(String start) {
		class TopologicalSorter {
			private final HashMap<String, HashSet<String>> nodes;
			private final LinkedList<String> result;
			private final HashMap<String, Short> visited;
			private final Short Visiting = 1;
			private final Short Visited = 2;

			TopologicalSorter(HashMap<String, HashSet<String>> nodes) {
				this.nodes = nodes;
				this.result = new LinkedList<>();
				this.visited = new HashMap<>();
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
								NezCC2Visitor2.this.crossRefNames.add(nextNode);
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
		TopologicalSorter sorter = new TopologicalSorter(this.depsMap);
		ArrayList<String> funcList = sorter.getResult();
		if (!funcList.contains(start)) {
			funcList.add(start);
		}
		// this.depsMap.clear();
		return funcList;
	}

	protected void log(String line, Object... args) {
		System.err.printf(line + "%n", args);
	}

}