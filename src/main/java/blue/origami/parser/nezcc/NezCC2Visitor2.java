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

package blue.origami.parser.nezcc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import blue.origami.parser.ParserGrammar;
import blue.origami.parser.ParserGrammar.MemoPoint;
import blue.origami.parser.peg.ByteSet;
import blue.origami.parser.peg.Expression;
import blue.origami.parser.peg.ExpressionVisitor;
import blue.origami.parser.peg.NonEmpty;
import blue.origami.parser.peg.PAnd;
import blue.origami.parser.peg.PAny;
import blue.origami.parser.peg.PByte;
import blue.origami.parser.peg.PByteSet;
import blue.origami.parser.peg.PChoice;
import blue.origami.parser.peg.PDetree;
import blue.origami.parser.peg.PDispatch;
import blue.origami.parser.peg.PEmpty;
import blue.origami.parser.peg.PFail;
import blue.origami.parser.peg.PIf;
import blue.origami.parser.peg.PLinkTree;
import blue.origami.parser.peg.PMany;
import blue.origami.parser.peg.PNonTerminal;
import blue.origami.parser.peg.PNot;
import blue.origami.parser.peg.POn;
import blue.origami.parser.peg.POption;
import blue.origami.parser.peg.PPair;
import blue.origami.parser.peg.PSymbolAction;
import blue.origami.parser.peg.PSymbolPredicate;
import blue.origami.parser.peg.PSymbolScope;
import blue.origami.parser.peg.PTag;
import blue.origami.parser.peg.PTrap;
import blue.origami.parser.peg.PTree;
import blue.origami.parser.peg.PValue;
import blue.origami.parser.peg.Production;
import blue.origami.parser.peg.Stateful;
import blue.origami.parser.peg.Typestate;

class NezCC2Visitor2 extends ExpressionVisitor<NezCC2.Expression, NezCC2> {
	final int mask;
	private final static int POS = NezCC2.POS;
	private final static int TREE = NezCC2.TREE;
	private final static int STATE = NezCC2.STATE;
	private final static int EMPTY = NezCC2.EMPTY;

	NezCC2Visitor2(int mask) {
		this.mask = mask;
	}

	final static boolean Function = true;
	final static boolean Inline = false;

	private ArrayList<Expression> waitingList = new ArrayList<>();

	public void start(ParserGrammar g, NezCC2 pg) {
		Production p = g.getStartProduction();
		this.g = g;
		if (pg.isDefined("comment")) {
			this.comment = pg.s("comment");
		}
		pg.declConst(pg.T("pos"), "memosize", "" + g.getMemoPointSize());
		pg.declConst(pg.T("pos"), "memolen", "" + g.getMemoPointSize() * 64 + 1);
		// pg.declConst(pg.T("length"), "memosize", -1, "" + g.getMemoPointSize());
		// pg.declConst(pg.T("length"), "memoentries", -1, "" + (g.getMemoPointSize() *
		// 64 + 1));
		this.log("memosize: %d", g.getMemoPointSize());
		int c = 0;
		this.waitingList.add(p.getExpression());
		for (int i = 0; i < this.waitingList.size(); i++) {
			Expression e0 = this.waitingList.get(i);
			String funcName = this.getFuncName(e0);
			MemoPoint memoPoint = null;
			if (e0 instanceof PNonTerminal) {
				memoPoint = g.getMemoPoint(((PNonTerminal) e0).getUniqueName());
				e0 = ((PNonTerminal) e0).getExpression();
			}
			if (!this.isDefinedSection(funcName)) {
				this.setCurrentFuncName(funcName);
				// pg.writeSection(pg.emitAsm(pg.format("comment", target)));
				this.define(funcName, pg.define(funcName, "px").add(this.eMemo(e0, memoPoint, pg)));
			}
			c++;
		}
		this.log("funcsize: %d", c);
		// this.declSymbolTables();
	}

	private NezCC2.Expression eMemo(Expression e, MemoPoint m, NezCC2 pg) {
		if (m == null) {
			return this.eMatch(e, false, pg);
		}
		int acc = (Typestate.compute(e) == Typestate.Tree ? POS | TREE : POS) | this.mask;
		String funcName = this.funcAcc("memo", acc);
		pg.used("getkey");
		pg.used("getmemo");
		pg.used("mconsume" + acc);
		pg.used("mstore" + acc);
		return pg.apply(funcName, "px", m.id + 1, this.getLambdaExpression(e, pg));
	}

	private NezCC2.Expression eMatch(Expression e, boolean asFunction, NezCC2 pg) {
		if (asFunction) {
			String funcName = this.getFuncName(e);
			this.waitingList.add(e);
			this.addFunctionDependency(this.getCurrentFuncName(), funcName);
			return pg.apply(funcName, "px");
		} else {
			return e.visit(this, pg);
		}
	}

	boolean alwaysFunc(Expression e) {
		if (e instanceof PChoice) {
			return e.size() > 2;
		}
		if (e instanceof PDispatch || e instanceof PNonTerminal) {
			return true;
		}
		return false;
	}

	private NezCC2.Expression eMatch(Expression e, NezCC2 pg) {
		return this.eMatch(e, this.alwaysFunc(e), pg);
	}

	@Override
	public NezCC2.Expression visitNonTerminal(PNonTerminal e, NezCC2 pg) {
		String funcName = this.getFuncName(e);
		this.waitingList.add(e);
		this.addFunctionDependency(this.getCurrentFuncName(), funcName);
		return pg.apply(funcName, "px");
	}

	@Override
	public NezCC2.Expression visitEmpty(PEmpty e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	private NezCC2.Expression eSucc(NezCC2 pg) {
		return pg.p("true");
	}

	@Override
	public NezCC2.Expression visitFail(PFail e, NezCC2 pg) {
		return this.eFail(pg);
	}

	private NezCC2.Expression eFail(NezCC2 pg) {
		return pg.p("false");
	}

	private NezCC2.Expression eNext(NezCC2 pg) {
		return pg.apply("mnext1", "px");
	}

	private NezCC2.Expression eNotEOF(NezCC2 pg) {
		return pg.apply("neof", "px");
	}

	@Override
	public NezCC2.Expression visitByte(PByte e, NezCC2 pg) {
		return this.eMatchByteNext(e.byteChar(), pg);
	}

	private NezCC2.Expression eMatchByte(int uchar, NezCC2 pg) {
		NezCC2.Expression expr = pg.p("px.inputs[px.pos] == $0", (char) (uchar & 0xff));
		if (uchar == 0) {
			expr = this.eNotEOF(pg).and(expr);
		}
		return expr;
	}

	private NezCC2.Expression eMatchByteNext(int uchar, NezCC2 pg) {
		if (pg.isDefined("inc")) {
			NezCC2.Expression expr = pg.p("px.inputs[inc!(px.pos)] == $0", (char) (uchar & 0xff));
			if (uchar == 0) {
				expr = this.eNotEOF(pg).and(expr);
			}
			return expr;
		}
		return this.eMatchByte(uchar, pg).and(this.eNext(pg));
	}

	@Override
	public NezCC2.Expression visitAny(PAny e, NezCC2 pg) {
		return this.eNotEOF(pg).and(this.eNext(pg));
	}

	@Override
	public NezCC2.Expression visitByteSet(PByteSet e, NezCC2 pg) {
		return this.eMatchByteSetNext(e.byteSet(), pg);
	}

	private NezCC2.Expression eMatchByteSet(ByteSet bs, NezCC2 pg) {
		int uchar = bs.getUnsignedByte();
		if (uchar != -1) {
			return this.eMatchByte(uchar, pg);
		}
		NezCC2.Expression index = pg.p("px.inputs[px.pos]");
		if (pg.isDefined("Obits32")) {
			return pg.apply("bits32", bs, index);
		} else {
			return pg.p("$0[unsigned!($1)]", bs, index);
		}
	}

	private NezCC2.Expression eMatchByteSetNext(ByteSet bs, NezCC2 pg) {
		int uchar = bs.getUnsignedByte();
		if (uchar != -1) {
			return this.eMatchByteNext(uchar, pg);
		}
		if (pg.isDefined("inc")) {
			NezCC2.Expression index = pg.p("px.inputs[inc!(px.pos)]");
			if (pg.isDefined("Obits32")) {
				return pg.apply("bits32", bs, index);
			} else {
				return pg.p("$0[unsigned!($1)]", bs, index);
			}
		}
		return this.eMatchByteSet(bs, pg).and(this.eNext(pg));
	}

	@Override
	public NezCC2.Expression visitPair(PPair e, NezCC2 pg) {
		if (pg.isDefined("Ostring")) {
			ArrayList<Integer> l = new ArrayList<>();
			Expression remain = Expression.extractMultiBytes(e, l);
			if (l.size() > 2) {
				byte[] text = Expression.toMultiBytes(l);
				NezCC2.Expression first = this.eMatchByteNext(text[0] & 0xff, pg);
				for (int i = 1; i < text.length; i++) {
					first = first.and(this.eMatchByteNext(text[i] & 0xff, pg));
				}
				if (!(remain instanceof PEmpty)) {
					first = first.and(this.eMatch(remain, pg));
				}
				return first;
			}
		}
		NezCC2.Expression pe1 = this.eMatch(e.get(0), pg);
		NezCC2.Expression pe2 = this.eMatch(e.get(1), pg);
		return pe1.and(pe2);
	}

	@Override
	public NezCC2.Expression visitChoice(PChoice e, NezCC2 pg) {
		if (e.size() == 2) {
			return this.eChoice(e.get(0), e.get(1), pg);
		} else {
			int acc = this.varStacks(POS, e);
			NezCC2.Expression main = this.eMatch(e.get(0), pg);
			for (int i = 1; i < e.size(); i++) {
				main = pg.p("($0 || $1 && $2)", main, this.eBack(pg, acc), this.eMatch(e.get(i), pg));
			}
			return this.eLetAccIn(acc, main, pg);
		}
	}

	NezCC2.Expression eChoice(Expression inner, Expression inner2, NezCC2 pg) {
		int acc = this.varStacks(POS, inner);
		NezCC2.Expression lambda = this.getLambdaExpression(inner, pg);
		int acc2 = this.varStacks(POS, inner2);
		NezCC2.Expression lambda2 = this.getLambdaExpression(inner2, pg);
		String func = this.funcAcc("choice", acc | acc2);
		return pg.apply(func, "px", lambda, lambda2);
	}

	@Override
	public NezCC2.Expression visitDispatch(PDispatch e, NezCC2 pg) {
		List<NezCC2.Expression> exprs = new ArrayList<>(e.size() + 1);
		exprs.add(this.eFail(pg));
		if (this.isAllConsumed(e) && pg.isDefined("inc")) {
			for (int i = 0; i < e.size(); i++) {
				Expression sub = e.get(i);
				if (sub instanceof PPair) {
					assert (!(sub.get(0) instanceof PPair));
					exprs.add(this.eMatch(sub.get(1), pg));
				} else {
					exprs.add(this.eSucc(pg));
				}
			}
			return pg.dispatch(this.eJumpIndex(e.indexMap, true, pg), exprs);
		} else {
			for (int i = 0; i < e.size(); i++) {
				exprs.add(this.patch(e.get(i), pg));
			}
			return pg.dispatch(this.eJumpIndex(e.indexMap, false, pg), exprs);
		}
	}

	protected NezCC2.Expression eJumpIndex(byte[] indexMap, boolean inc, NezCC2 pg) {
		NezCC2.Expression index = inc ? pg.p("inc!(px.pos)") : pg.p("px.pos");
		boolean hasMinusIndex = false;
		for (byte b : indexMap) {
			if (b < 0) {
				hasMinusIndex = true;
				break;
			}
		}
		if (hasMinusIndex) {
			return pg.p("$0[toindex!(px.inputs[$1])]", indexMap, index);
		}
		return pg.p("$0[px.inputs[$1]]", indexMap, index);
	}

	private boolean isAllConsumed(PDispatch e) {
		for (Expression sub : e) {
			if (!this.isConsumed(sub)) {
				return false;
			}
		}
		return e.indexMap[0] == 0;
	}

	private boolean isConsumed(Expression first) {
		if (first instanceof PPair) {
			return this.isConsumed(first.get(0));
		}
		if (first instanceof PAny || first instanceof PByte || first instanceof PByteSet) {
			return true;
		}
		return false;
	}

	private NezCC2.Expression patch(Expression e, NezCC2 pg) {
		if (e instanceof PPair) {
			Expression first = e.get(0);
			if (first instanceof PAny || first instanceof PByte || first instanceof PByteSet) {
				return pg.p("mnext(px) && $0", this.eMatch(e.get(1), pg));
			}
		}
		return this.eMatch(e, pg);
	}

	@Override
	public NezCC2.Expression visitOption(POption e, NezCC2 pg) {
		return this.eOption(e.get(0), pg);
	}

	NezCC2.Expression eOption(Expression inner, NezCC2 pg) {
		int acc = this.varStacks(POS, inner);
		NezCC2.Expression lambda = this.getLambdaExpression(inner, pg);
		String func = this.funcAcc("choice", acc);
		return pg.apply(func, "px", lambda, pg.unary("^succ"));
	}

	@Override
	public NezCC2.Expression visitMany(PMany e, NezCC2 pg) {
		return this.eMany(e.isOneMore(), e.get(0), pg);
	}

	private NezCC2.Expression eMany(boolean isOneMore, Expression inner, NezCC2 pg) {
		NezCC2.Expression lambda = this.getLambdaExpression(inner, pg);
		int acc = this.varStacks(POS, inner);
		if (!NonEmpty.isAlwaysConsumed(inner)) {
			acc |= EMPTY;
		}
		String func = this.funcAcc("many", acc);
		pg.used("mback" + acc);
		NezCC2.Expression e = pg.apply(func, "px", lambda);
		return (isOneMore) ? this.eMatch(inner, pg).and(e) : e;
	}

	@Override
	public NezCC2.Expression visitAnd(PAnd e, NezCC2 pg) {
		return this.eAnd(e.get(0), pg);
	}

	private NezCC2.Expression eAnd(Expression inner, NezCC2 pg) {
		Expression p = Expression.deref(inner);
		if (p instanceof PAny) {
			return pg.apply("neof", "px");
		}
		// ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		// if (bs != null) {
		// this.funcAcc("getbyte");
		// return pg.emitMatchByteSet(bs, null, false);
		// }
		NezCC2.Expression lambda = this.getLambdaExpression(inner, pg);
		pg.used("mback1");
		return pg.apply("and1", "px", lambda);
	}

	@Override
	public NezCC2.Expression visitNot(PNot e, NezCC2 pg) {
		return this.eNot(e.get(0), pg);
	}

	private NezCC2.Expression eNot(Expression inner, NezCC2 pg) {
		int acc = this.varStacks(POS, inner);
		Expression p = Expression.deref(inner);
		if (p instanceof PAny) {
			return pg.p("!neof(px)");
		}
		// ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		// if (bs != null) {
		// bs = bs.not(pg.isBinary());
		// this.funcAcc("getbyte");
		// NezCC2.Expression expr = pg.emitMatchByteSet(bs, null, false);
		// return expr;
		// }
		NezCC2.Expression lambda = this.getLambdaExpression(inner, pg);
		String func = this.funcAcc("not", acc);
		pg.used("mback" + acc);
		return pg.apply(func, "px", lambda);
	}

	private NezCC2.Expression getLambdaExpression(Expression inner, NezCC2 pg) {
		if (pg.isDefined("lambda")) {
			if (this.alwaysFunc(inner) && pg.isDefined("funcref")) {
				this.eMatch(inner, true, pg);
				return pg.p("^" + this.getFuncName(inner));
			}
			NezCC2.Expression e = this.eMatch(inner, false, pg);
			return pg.apply(null, "px", e);
		} else {
			this.eMatch(inner, true, pg);
			return pg.p("^" + this.getFuncName(inner));
		}
	}

	@Override
	public NezCC2.Expression visitTree(PTree e, NezCC2 pg) {
		NezCC2.Expression inner = this.getLambdaExpression(e.get(0), pg);
		pg.used("mtree");
		if (e.folding) {
			pg.used("mlink");
			return pg.apply("foldtree", "px", e.beginShift, e.label, inner, e.tag, e.endShift);
		}
		return pg.apply("newtree", "px", e.beginShift, inner, e.tag, e.endShift);
	}

	@Override
	public NezCC2.Expression visitDetree(PDetree e, NezCC2 pg) {
		return this.eDetree(e, pg);
	}

	private NezCC2.Expression eDetree(PDetree e, NezCC2 pg) {
		NezCC2.Expression lambda = this.getLambdaExpression(e.get(0), pg);
		pg.used("mback3");
		return pg.apply("detree", "px", lambda);
	}

	@Override
	public NezCC2.Expression visitLinkTree(PLinkTree e, NezCC2 pg) {
		return this.eLink(e, pg);
	}

	private NezCC2.Expression eLink(PLinkTree e, NezCC2 pg) {
		NezCC2.Expression lambda = this.getLambdaExpression(e.get(0), pg);
		pg.used("mlink");
		return pg.apply("linktree", "px", e.label, lambda);
	}

	@Override
	public NezCC2.Expression visitTag(PTag e, NezCC2 pg) {
		pg.used("mlink");
		return pg.apply("tagtree", "px", e.tag);
	}

	@Override
	public NezCC2.Expression visitValue(PValue e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.Expression visitSymbolScope(PSymbolScope e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.Expression visitSymbolAction(PSymbolAction e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.Expression visitSymbolPredicate(PSymbolPredicate e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.Expression visitIf(PIf e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.Expression visitOn(POn e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.Expression visitTrap(PTrap e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	//

	private int u = 0;

	private int varStacks(int acc, Expression e) {
		if (Typestate.compute(e) != Typestate.Unit) {
			acc |= TREE;
		}
		if (Stateful.isStateful(e)) {
			acc |= (STATE | TREE);
		}
		return acc;
	}

	private String funcAcc(String f, int acc) {
		return f + acc;
	}

	private NezCC2.Expression eLetAccIn(int acc, NezCC2.Expression e, NezCC2 pg) {
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

	// NezCC2.Expression emitUpdate(NezCC2 pg, int acc) {
	// B block = pg.beginBlock();
	// for (String n : pg.getStackNames(acc & ~(CNT | EMPTY))) {
	// pg.emitStmt(block, pg.emitAssign(n, pg.emitGetter(n)));
	// }
	// if ((acc & CNT) == CNT) {
	// pg.emitStmt(block, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+",
	// pg.vInt(1))));
	// }
	// return pg.endBlock(block);
	// }

	private String[] accNames(int acc, String... prefixes) {
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
		return l.toArray(new String[l.size()]);
	}

	private NezCC2.Expression eBack(NezCC2 pg, int acc) {
		String funcName = "back" + (acc & ~(EMPTY));
		String[] args = this.accNames(acc & ~(EMPTY), "px");
		return pg.apply(funcName, args);
	}

	// function
	ParserGrammar g;
	String comment = "/*%s*/";
	HashMap<String, String> exprFuncMap = new HashMap<>();
	HashMap<String, String> termMap = new HashMap<>();

	private String getFuncName(Expression e) {
		String suffix = "";
		if (e instanceof PNonTerminal) {
			String uname = ((PNonTerminal) e).getUniqueName();
			MemoPoint memoPoint = this.g.getMemoPoint(((PNonTerminal) e).getUniqueName());
			suffix = String.format(this.comment, uname);
			if (memoPoint != null) {
				String fname = this.exprFuncMap.get(uname);
				if (fname == null) {
					fname = "e" + this.exprFuncMap.size() + suffix;
					this.exprFuncMap.put(uname, fname);
				}
				return fname;
			}
			e = ((PNonTerminal) e).getExpression();
		}
		String key = e.toString();
		String name = this.exprFuncMap.get(key);
		if (name == null) {
			name = "e" + this.exprFuncMap.size() + suffix;
			this.exprFuncMap.put(key, name);
		}
		return name;
	}

	private String getFuncName(MemoPoint mp, Expression e) {
		String key = e.toString();
		String name = this.exprFuncMap.get(key);
		if (name == null) {
			name = "e" + this.exprFuncMap.size();
			this.exprFuncMap.put(key, name);
		}
		return name;
	}

	protected HashMap<String, String> funcMap = new HashMap<>();

	boolean isDefinedSection(String funcName) {
		return this.funcMap.containsKey(funcName);
	}

	void define(String funcName, NezCC2.Expression funcBody) {
		this.funcMap.put(funcName, funcBody.toString());
	}

	String getParseFunc(String funcName) {
		return this.funcMap.get(funcName);
	}

	private String currentFuncName = null;

	void setCurrentFuncName(String funcName) {
		this.currentFuncName = funcName;
		this.u = 0;
	}

	String getCurrentFuncName() {
		return this.currentFuncName;
	}

	HashSet<String> crossRefNames = new HashSet<>();
	HashMap<String, HashSet<String>> depsMap = new HashMap<>();

	private final void addFunctionDependency(String sour, String dest) {
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