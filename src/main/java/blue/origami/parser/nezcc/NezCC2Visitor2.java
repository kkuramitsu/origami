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

import blue.origami.common.Symbol;
import blue.origami.parser.ParserGrammar;
import blue.origami.parser.ParserGrammar.MemoPoint;
import blue.origami.parser.nezcc.NezCC2.ENode;
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

class NezCC2Visitor2 extends ExpressionVisitor<NezCC2.ENode, NezCC2> {
	final int mask;
	final static int POS = NezCC2.POS;
	final static int TREE = NezCC2.TREE;
	final static int STATE = NezCC2.STATE;
	final static int EMPTY = NezCC2.EMPTY;

	NezCC2Visitor2(int mask) {
		this.mask = mask;
	}

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
		this.log("mask: %d", this.mask);
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
	}

	private NezCC2.ENode eMemo(Expression e, MemoPoint m, NezCC2 pg) {
		if (m == null || !pg.isDefined("Omemo")) {
			return this.eMatch(e, false, pg);
		}
		int acc = this.mask;
		String funcName = this.funcAcc("memo", acc);
		pg.used("getkey");
		pg.used("getmemo");
		pg.used("mconsume" + acc);
		pg.used("mstore" + acc);
		return pg.apply(funcName, "px", m.id + 1, this.eLambda(e, pg));
	}

	private NezCC2.ENode eMatch(Expression e, boolean asFunction, NezCC2 pg) {
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
			return e.size() > 3;
		}
		if (e instanceof PDispatch || e instanceof PNonTerminal) {
			return true;
		}
		return false;
	}

	private NezCC2.ENode eMatch(Expression e, NezCC2 pg) {
		return this.eMatch(e, this.alwaysFunc(e), pg);
	}

	@Override
	public NezCC2.ENode visitNonTerminal(PNonTerminal e, NezCC2 pg) {
		String funcName = this.getFuncName(e);
		this.waitingList.add(e);
		this.addFunctionDependency(this.getCurrentFuncName(), funcName);
		return pg.apply(funcName, "px");
	}

	@Override
	public NezCC2.ENode visitEmpty(PEmpty e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	private NezCC2.ENode eSucc(NezCC2 pg) {
		return pg.p("true");
	}

	@Override
	public NezCC2.ENode visitFail(PFail e, NezCC2 pg) {
		return this.eFail(pg);
	}

	private NezCC2.ENode eFail(NezCC2 pg) {
		return pg.p("false");
	}

	private NezCC2.ENode eNext(NezCC2 pg) {
		return pg.apply("mnext1", "px");
	}

	private NezCC2.ENode eNotEOF(NezCC2 pg) {
		return pg.p("px.pos < px.length");
	}

	private NezCC2.ENode eNotEOFNext(NezCC2 pg) {
		if (pg.isDefined("posinc")) {
			return pg.p("px.length < posinc!(px)");
		}
		return this.eNotEOF(pg).and(this.eNext(pg));
	}

	@Override
	public NezCC2.ENode visitByte(PByte e, NezCC2 pg) {
		return this.eMatchByteNext(e.byteChar(), pg);
	}

	private NezCC2.ENode eMatchByte(int uchar, NezCC2 pg) {
		NezCC2.ENode expr = pg.p("px.inputs[px.pos] == $0", (char) (uchar & 0xff));
		if (uchar == 0) {
			expr = this.eNotEOF(pg).and(expr);
		}
		return expr;
	}

	private NezCC2.ENode eMatchByteNext(int uchar, NezCC2 pg) {
		if (pg.isDefined("posinc")) {
			NezCC2.ENode expr = pg.p("px.inputs[posinc!(px)] == $0", (char) (uchar & 0xff));
			if (uchar == 0) {
				expr = this.eNotEOF(pg).and(expr);
			}
			return expr;
		}
		return this.eMatchByte(uchar, pg).and(this.eNext(pg));
	}

	@Override
	public NezCC2.ENode visitAny(PAny e, NezCC2 pg) {
		return this.eNotEOF(pg).and(this.eNext(pg));
	}

	@Override
	public NezCC2.ENode visitByteSet(PByteSet e, NezCC2 pg) {
		return this.eMatchByteSetNext(e.byteSet(), pg);
	}

	private NezCC2.ENode eMatchByteSet(ByteSet bs, NezCC2 pg) {
		int uchar = bs.getUnsignedByte();
		if (uchar != -1) {
			return this.eMatchByte(uchar, pg);
		}
		NezCC2.ENode index = pg.p("px.inputs[px.pos]");
		if (pg.isDefined("Obits32")) {
			return pg.apply("bits32", bs, index);
		} else {
			return pg.p("$0[unsigned!($1)]", bs, index);
		}
	}

	private NezCC2.ENode eMatchByteSetNext(ByteSet bs, NezCC2 pg) {
		int uchar = bs.getUnsignedByte();
		if (uchar != -1) {
			return this.eMatchByteNext(uchar, pg);
		}
		if (pg.isDefined("posinc")) {
			NezCC2.ENode index = pg.p("px.inputs[posinc!(px)]");
			if (pg.isDefined("Obits32")) {
				return pg.apply("bits32", bs, index);
			} else {
				return pg.p("$0[unsigned!($1)]", bs, index);
			}
		}
		return this.eMatchByteSet(bs, pg).and(this.eNext(pg));
	}

	private NezCC2.ENode eMatchBytes(byte[] b, NezCC2 pg) {
		return this.eMatchBytes(b, 0, pg);
	}

	private NezCC2.ENode eMatchBytes(byte[] b, int offset, NezCC2 pg) {
		int len = b.length - offset;
		// System.err.println("offset=" + offset + ",b=" + b.length);
		if (len == 1) {
			return this.eMatchByte(b[0] & 0xff, pg);
		}
		if (len == 2) {
			return pg.apply("match2", "px", (char) b[offset], (char) b[offset + 1]);
		}
		if (len == 3) {
			return pg.apply("match3", "px", (char) b[offset], (char) b[offset + 1], (char) b[offset + 2]);
		}
		if (len == 4) {
			return pg.apply("match4", "px", (char) b[offset], (char) b[offset + 1], (char) b[offset + 2],
					(char) b[offset + 3]);
		}
		return pg.apply("matchmany", "px.inputs", "px.pos", pg.data(b), 0, b.length);
	}

	private NezCC2.ENode eMatchBytesNext(byte[] b, NezCC2 pg) {
		return this.eMatchBytes(b, 0, pg).and(pg.apply("mmov", "px", b.length));
	}

	@Override
	public NezCC2.ENode visitPair(PPair e, NezCC2 pg) {
		ArrayList<Integer> l = new ArrayList<>();
		Expression remain = Expression.extractMultiBytes(e, l);
		if (l.size() > 1) {
			byte[] text = Expression.toMultiBytes(l);
			NezCC2.ENode first = this.eMatchBytesNext(text, pg);
			if (!(remain instanceof PEmpty)) {
				first = first.and(this.eMatch(remain, pg));
			}
			return first;
		}
		NezCC2.ENode pe1 = this.eMatch(e.get(0), pg);
		NezCC2.ENode pe2 = this.eMatch(e.get(1), pg);
		return pe1.and(pe2);
	}

	@Override
	public NezCC2.ENode visitChoice(PChoice e, NezCC2 pg) {
		if (e.size() == 2) {
			return this.eChoice(e.get(0), e.get(1), pg);
		} else if (e.size() == 3) {
			return this.eChoice3(e.get(0), e.get(1), e.get(2), pg);
		} else {
			// System.err.println("choice " + e.size());
			int acc = this.varStacks(POS, e);
			NezCC2.ENode main = this.eMatch(e.get(0), pg);
			for (int i = 1; i < e.size(); i++) {
				main = pg.p("($0 || $1 && $2)", main, this.eBack(pg, acc), this.eMatch(e.get(i), pg));
			}
			return this.eLetAccIn(acc, main, pg);
		}
	}

	NezCC2.ENode eChoice(Expression inner, Expression inner2, NezCC2 pg) {
		int acc = this.varStacks(POS, inner);
		NezCC2.ENode lambda = this.eLambda(inner, pg);
		int acc2 = this.varStacks(POS, inner2);
		NezCC2.ENode lambda2 = this.eLambda(inner2, pg);
		String func = this.funcAcc("or", acc | acc2);
		pg.used("mback" + (acc | acc2));
		return pg.apply(func, "px", lambda, lambda2);
	}

	NezCC2.ENode eChoice3(Expression inner, Expression inner2, Expression inner3, NezCC2 pg) {
		int acc = this.varStacks(POS, inner);
		NezCC2.ENode lambda = this.eLambda(inner, pg);
		int acc2 = this.varStacks(POS, inner2);
		NezCC2.ENode lambda2 = this.eLambda(inner2, pg);
		int acc3 = this.varStacks(POS, inner3);
		NezCC2.ENode lambda3 = this.eLambda(inner3, pg);
		pg.used("mback" + (acc | acc2 | acc3));
		String func = this.funcAcc("oror", acc | acc2 | acc3);
		return pg.apply(func, "px", lambda, lambda2, lambda3);
	}

	@Override
	public NezCC2.ENode visitDispatch(PDispatch e, NezCC2 pg) {
		boolean hasJumpTable = pg.isDefined("Ojumptable");
		List<NezCC2.ENode> exprs = new ArrayList<>(e.size() + 1);
		ENode index = null;
		exprs.add(hasJumpTable ? pg.p("^fail") : this.eFail(pg));
		if (this.isAllConsumed(e) && pg.isDefined("posinc")) {
			for (int i = 0; i < e.size(); i++) {
				Expression sub = e.get(i);
				if (sub instanceof PPair) {
					assert (!(sub.get(0) instanceof PPair));
					exprs.add(hasJumpTable ? this.eLambda(sub.get(1), pg) : this.eMatch(sub.get(1), pg));
				} else {
					exprs.add(hasJumpTable ? pg.p("^succ") : this.eSucc(pg));
				}
			}
			index = this.eJumpIndex(e.indexMap, true, pg);
		} else {
			for (int i = 0; i < e.size(); i++) {
				exprs.add(this.eSkipFirst(e.get(i), hasJumpTable, pg));
			}
			index = this.eJumpIndex(e.indexMap, false, pg);
		}
		return pg.dispatch(index, exprs);
	}

	protected NezCC2.ENode eJumpIndex(byte[] indexMap, boolean inc, NezCC2 pg) {
		NezCC2.ENode index = inc ? pg.p("posinc!(px)") : pg.p("px.pos");
		boolean hasMinusIndex = false;
		for (byte b : indexMap) {
			if (b < 0) {
				hasMinusIndex = true;
				break;
			}
		}
		if (hasMinusIndex) {
			return pg.p("unsigned!($0[unsigned!(px.inputs[$1])])", indexMap, index);
		}
		return pg.p("$0[unsigned!(px.inputs[$1])]", indexMap, index);
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

	private NezCC2.ENode eSkipFirst(Expression e, boolean hasJumpTable, NezCC2 pg) {
		if (e instanceof PPair && (hasJumpTable && pg.isDefined("lambda"))) {
			Expression first = e.get(0);
			if (first instanceof PAny || first instanceof PByte || first instanceof PByteSet) {
				ENode skiped = this.eNext(pg).and(this.eMatch(e.get(1), pg));
				return hasJumpTable ? pg.apply(null, "px", skiped) : skiped;
			}
		}
		return hasJumpTable ? this.eLambda(e, pg) : this.eMatch(e, pg);
	}

	@Override
	public NezCC2.ENode visitOption(POption e, NezCC2 pg) {
		return this.eOption(e.get(0), pg);
	}

	NezCC2.ENode eOption(Expression inner, NezCC2 pg) {
		Expression p = Expression.deref(inner);
		if (p instanceof PAny) {
			// System.err.println("lex: ?" + inner);
			return this.eNotEOFNext(pg);
		}
		ByteSet bs = Expression.getByteSet(p);
		if (bs != null) {
			// System.err.println("lex: ?" + inner);
			return this.eMatchByteSetNext(bs, pg);
		}
		byte[] text = Expression.getBytes(p);
		if (text != null) {
			return this.eMatchBytesNext(text, pg).or(this.eSucc(pg));
		}
		int acc = this.varStacks(POS, inner);
		NezCC2.ENode lambda = this.eLambda(inner, pg);
		String func = this.funcAcc("maybe", acc);
		pg.used("mback" + acc);
		return pg.apply(func, "px", lambda);
	}

	@Override
	public NezCC2.ENode visitMany(PMany e, NezCC2 pg) {
		return this.eMany(e.isOneMore(), e.get(0), pg);
	}

	private NezCC2.ENode eMany(Expression inner, NezCC2 pg) {
		Expression p = Expression.deref(inner);
		if (p instanceof PAny) {
			// System.err.println("lex: *" + inner);
			return pg.apply("manyany", "px");
		}
		ByteSet bs = Expression.getByteSet(p);
		if (bs != null) {
			// System.err.println("lex: *" + inner);
			return pg.apply("manychar", "px", bs);
		}
		byte[] text = Expression.getBytes(p);
		if (text != null) {
			// System.err.println("lex: *" + inner);
			return pg.apply("manystr", "px", pg.data(text), text.length);
		}
		NezCC2.ENode lambda = this.eLambda(inner, pg);
		int acc = this.varStacks(POS, inner);
		pg.used("mback" + acc);
		if (!NonEmpty.isAlwaysConsumed(inner)) {
			acc |= EMPTY;
		}
		String func = this.funcAcc("many", acc);
		return pg.apply(func, "px", lambda);
	}

	private NezCC2.ENode eMany(boolean isOneMore, Expression inner, NezCC2 pg) {
		return (isOneMore) ? this.eMatch(inner, pg).and(this.eMany(inner, pg)) : this.eMany(inner, pg);
	}

	@Override
	public NezCC2.ENode visitAnd(PAnd e, NezCC2 pg) {
		return this.eAnd(e.get(0), pg);
	}

	private NezCC2.ENode eAnd(Expression inner, NezCC2 pg) {
		Expression p = Expression.deref(inner);
		if (p instanceof PAny) {
			return this.eNotEOF(pg);
		}
		ByteSet bs = Expression.getByteSet(p);
		if (bs != null) {
			return this.eMatchByteSet(bs, pg);
		}
		byte[] text = Expression.getBytes(p);
		if (text != null) {
			return this.eMatchBytes(text, pg);
		}
		NezCC2.ENode lambda = this.eLambda(inner, pg);
		pg.used("mback1");
		return pg.apply("and1", "px", lambda);
	}

	@Override
	public NezCC2.ENode visitNot(PNot e, NezCC2 pg) {
		return this.eNot(e.get(0), pg);
	}

	private NezCC2.ENode eNot(Expression inner, NezCC2 pg) {
		Expression p = Expression.deref(inner);
		if (p instanceof PAny) {
			System.err.println("lex: !" + inner);
			return pg.p("!($0)", this.eNotEOF(pg));
		}
		ByteSet bs = Expression.getByteSet(p);
		if (bs != null) {
			// System.err.println("lex: !" + inner);
			return pg.p("!($0)", this.eMatchByteSet(bs, pg));
		}
		byte[] text = Expression.getBytes(p);
		if (text != null) {
			// System.err.println("lex: !" + inner);
			return pg.p("!($0)", this.eMatchBytes(text, pg));
		}
		// System.err.println("lex: !" + inner);
		int acc = this.varStacks(POS, inner);
		NezCC2.ENode lambda = this.eLambda(inner, pg);
		String func = this.funcAcc("not", acc);
		pg.used("mback" + acc);
		return pg.apply(func, "px", lambda);
	}

	private NezCC2.ENode eLambda(Expression inner, NezCC2 pg) {
		if (pg.isDefined("lambda")) {
			if (this.alwaysFunc(inner) && pg.isDefined("funcref")) {
				this.eMatch(inner, true, pg);
				return pg.p("^" + this.getFuncName(inner));
			}
			NezCC2.ENode e = this.eMatch(inner, false, pg);
			return pg.apply(null, "px", e);
		} else {
			this.eMatch(inner, true, pg);
			return pg.p("^" + this.getFuncName(inner));
		}
	}

	private boolean isTreeConstruction() {
		return (this.mask & TREE) == TREE;
	}

	@Override
	public NezCC2.ENode visitTree(PTree e, NezCC2 pg) {
		if (this.isTreeConstruction()) {
			NezCC2.ENode inner = this.eLambda(e.get(0), pg);
			pg.used("mtree");
			if (e.folding) {
				pg.used("mlink");
				return pg.apply("foldtree", "px", e.beginShift, e.label, inner, e.tag, e.endShift);
			}
			return pg.apply("newtree", "px", e.beginShift, inner, e.tag, e.endShift);
		}
		return this.eMatch(e.get(0), pg);
	}

	@Override
	public NezCC2.ENode visitDetree(PDetree e, NezCC2 pg) {
		if (this.isTreeConstruction()) {
			return this.eDetree(e, pg);
		}
		return this.eMatch(e.get(0), pg);
	}

	private NezCC2.ENode eDetree(PDetree e, NezCC2 pg) {
		NezCC2.ENode lambda = this.eLambda(e.get(0), pg);
		pg.used("mback2");
		return pg.apply("detree", "px", lambda);
	}

	@Override
	public NezCC2.ENode visitLinkTree(PLinkTree e, NezCC2 pg) {
		return this.eLink(e, pg);
	}

	private NezCC2.ENode eLink(PLinkTree e, NezCC2 pg) {
		if (this.isTreeConstruction()) {
			NezCC2.ENode lambda = this.eLambda(e.get(0), pg);
			pg.used("mlink");
			return pg.apply("linktree", "px", e.label, lambda);
		}
		return this.eMatch(e.get(0), pg);
	}

	@Override
	public NezCC2.ENode visitTag(PTag e, NezCC2 pg) {
		if (this.isTreeConstruction()) {
			pg.used("mlink");
			return pg.apply("tagtree", "px", e.tag);
		} else {
			return this.eSucc(pg);
		}
	}

	@Override
	public NezCC2.ENode visitValue(PValue e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.ENode visitSymbolScope(PSymbolScope e, NezCC2 pg) {
		NezCC2.ENode lambda = this.eLambda(e.get(0), pg);
		pg.used("mback4");
		// if (e.label != null) {
		// return pg.apply("sreset4", "px", this.labelId(e.label));
		// }
		return pg.apply("scope4", "px", lambda);
	}

	private HashMap<String, Integer> labelMap = new HashMap<>();

	int labelId(Symbol id) {
		Integer n = this.labelMap.get(id.toString());
		if (n == null) {
			n = this.labelMap.size() + 1;
			this.labelMap.put(id.toString(), n);
		}
		return n;
	}

	@Override
	public NezCC2.ENode visitSymbolAction(PSymbolAction e, NezCC2 pg) {
		NezCC2.ENode lambda = this.eLambda(e.get(0), pg);
		pg.used("mstate");
		return pg.apply("symbol4", "px", this.labelId(e.label), lambda);
	}

	@Override
	public NezCC2.ENode visitSymbolPredicate(PSymbolPredicate e, NezCC2 pg) {
		pg.used("getstate");
		switch (e.getFunctionName()) {
		case "match":
			pg.used("matchmany");
			return pg.apply("smatch4", "px", this.labelId(e.label));
		case "exists":
			return pg.apply("sexists4", "px", this.labelId(e.label));
		case "equals":
			pg.used("matchmany");
			return pg.apply("sequals4", "px", this.labelId(e.label), this.eLambda(e.get(0), pg));
		case "contains":
			pg.used("smany");
			pg.used("matchmany");
			return pg.apply("scontains4", "px", this.labelId(e.label), this.eLambda(e.get(0), pg));
		default:
			return this.eSucc(pg);
		}
	}

	@Override
	public NezCC2.ENode visitIf(PIf e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.ENode visitOn(POn e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	@Override
	public NezCC2.ENode visitTrap(PTrap e, NezCC2 pg) {
		return this.eSucc(pg);
	}

	//

	// private int u = 0;

	protected int varStacks(int acc, Expression e) {
		if (Typestate.compute(e) != Typestate.Unit) {
			acc |= TREE;
		}
		if (Stateful.isStateful(e)) {
			acc |= (STATE | TREE);
		}
		return acc & this.mask;
	}

	private String funcAcc(String f, int acc) {
		return f + acc;
	}

	private NezCC2.ENode eLetAccIn(int acc, NezCC2.ENode e, NezCC2 pg) {
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

	private NezCC2.ENode eBack(NezCC2 pg, int acc) {
		String funcName = "mback" + (acc & ~(EMPTY));
		Object[] args = this.accNames(acc & ~(EMPTY), "px");
		return pg.apply(funcName, args);
	}

	// function
	ParserGrammar g;
	String comment = "";
	HashMap<String, String> exprFuncMap = new HashMap<>();
	HashMap<String, String> termMap = new HashMap<>();

	protected String getFuncName(Expression e) {
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

	// private String getFuncName(MemoPoint mp, Expression e) {
	// String key = e.toString();
	// String name = this.exprFuncMap.get(key);
	// if (name == null) {
	// name = "e" + this.exprFuncMap.size();
	// this.exprFuncMap.put(key, name);
	// }
	// return name;
	// }

	protected HashMap<String, String> funcMap = new HashMap<>();

	boolean isDefinedSection(String funcName) {
		return this.funcMap.containsKey(funcName);
	}

	void define(String funcName, NezCC2.ENode funcBody) {
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