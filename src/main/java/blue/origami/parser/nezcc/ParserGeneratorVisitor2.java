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

import blue.origami.common.OConsole;
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

class ParserGeneratorVisitor2 extends ExpressionVisitor<NezCC2.Expression, NezCC2> {
	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;
	final static int CNT = 1 << 3;
	final static int EMPTY = 1 << 4;

	final static boolean Function = true;
	final static boolean Inline = false;

	private ArrayList<Expression> waitingList = new ArrayList<>();

	public void start(ParserGrammar g, NezCC2 pg) {
		Production p = g.getStartProduction();
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
				final Expression target = e0;
				final MemoPoint memo = memoPoint;
				this.setCurrentFuncName(funcName);
				// pg.writeSection(pg.emitAsm(pg.format("comment", target)));
				this.define(funcName, pg.define(funcName, "px").is(this.match(target, pg, memo)));
			}
			c++;
		}
		this.log("funcsize: %d", c);
		// this.declSymbolTables();
	}

	private NezCC2.Expression match(Expression e, NezCC2 pg, MemoPoint m) {
		if (m == null) {
			return this.match(e, pg, false);
		}
		String funcName = this.funcAcc("memo", Typestate.compute(e) == Typestate.Tree ? TREE : POS);
		return pg.apply(funcName, "px", m.id + 1, this.getInFunc(e, pg));
	}

	private NezCC2.Expression match(Expression e, NezCC2 pg, boolean asFunction) {
		if (asFunction) {
			String funcName = this.getFuncName(e);
			this.waitingList.add(e);
			this.addFunctionDependency(this.getCurrentFuncName(), funcName);
			return pg.p(funcName + "(px)");
		} else {
			return e.visit(this, pg);
		}
	}

	private NezCC2.Expression match(Expression e, NezCC2 pg) {
		NezCC2.Expression inline = null;
		if (e instanceof PMany) {
			int acc = this.varStacks(POS, e.get(0));
			inline = this.emitCombiMany(acc, e, pg);
		} else if (e instanceof PNot) {
			int acc = this.varStacks(POS, e.get(0));
			inline = this.emitCombiNot(acc, e, pg);
		} else if (e instanceof PLinkTree) {
			inline = this.emitCombiLink((PLinkTree) e, pg);
		} else if (e instanceof POption) {
			int acc = this.varStacks(POS, e.get(0));
			inline = this.emitCombiOption(acc, e, pg);
		}
		// if (e instanceof PChoice) {
		// int acc = this.varStacks (POS, e);
		// inline = this.emitInlineChoice(acc, e, pg);
		// }
		else if (e instanceof PAnd) {
			inline = this.emitCombiAnd(POS, e, pg);
		}
		if (inline != null && pg.isDefined("Oinline")) {
			return inline;
		}
		return this.match(e, pg, !this.isInline(e, pg));
	}

	private boolean isInline(Expression e, NezCC2 pg) {
		if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny || e instanceof PTree
				|| e instanceof PPair) {
			return true;
		}
		if (e instanceof PTag || e instanceof PValue || e instanceof PEmpty || e instanceof PFail) {
			return true;
		}
		return false;
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

	@Override
	public NezCC2.Expression visitByte(PByte e, NezCC2 pg) {
		return this.eMatchByte(e.byteChar(), pg).and(this.eNext(pg));
	}

	private NezCC2.Expression eMatchByte(int uchar, NezCC2 pg) {
		NezCC2.Expression expr = pg.p("px.inputs[px.pos] == $0", (char) (uchar & 0xff));
		if (uchar == 0) {
			expr = pg.p("neof(px) && $0", expr);
		}
		return expr;
	}

	private NezCC2.Expression eNext(NezCC2 pg) {
		return pg.apply("mnext1", "px");
	}

	@Override
	public NezCC2.Expression visitAny(PAny e, NezCC2 pg) {
		return pg.p("neof(px) && mnext1(px)");
	}

	@Override
	public NezCC2.Expression visitByteSet(PByteSet e, NezCC2 pg) {
		// ByteSet bs = e.byteSet();
		// int uchar = bs.getUnsignedByte();
		// if (uchar != -1) {
		// if (param == null) {
		// param = this.emitChar(uchar);
		// }
		// if (proceed) {
		// expr = this.apply("next1", this.V("px"), param);
		// } else {
		// expr = this.emitOp(this.apply("getbyte", this.V("px")), "==", param);
		// }
		// if (param == null) {
		// param = this.vByteSet(bs);
		// }
		// expr = this.apply(proceed ? "nextbyte" : "getbyte", this.V("px"));
	}

	private NezCC2.Expression eMatchByteSet(ByteSet bs, NezCC2 pg) {
		if (pg.isDefined("Int32")) {
			return this.apply("bits32", param, expr);
		} else {
			return this.emitArrayIndex(param, expr);
		}
	}

	@Override
	public NezCC2.Expression visitPair(PPair e, NezCC2 pg) {
		// FIXME
		// if (pg.useMultiBytes()) {
		// ArrayList<Integer> l = new ArrayList<>();
		// Expression remain = Expression.extractMultiBytes(e, l);
		// if (l.size() > 2) {
		// byte[] text = Expression.toMultiBytes(l);
		// NezCC2.Expression match = pg.matchBytes(text, true);
		// if (!(remain instanceof PEmpty)) {
		// match = pg.emitAnd(match, this.match(remain, pg));
		// }
		// return match;
		// }
		// }
		NezCC2.Expression pe1 = this.match(e.get(0), pg);
		NezCC2.Expression pe2 = this.match(e.get(1), pg);
		return pe1.and(pe2);
	}

	@Override
	public NezCC2.Expression visitChoice(PChoice e, NezCC2 pg) {
		int acc = this.varStacks(POS, e);
		NezCC2.Expression main = this.match(e.get(0), pg);
		for (int i = 1; i < e.size(); i++) {
			main = pg.p("($0 || $1 && $2)", main, this.mback(pg, acc), this.match(e.get(i), pg));
		}
		return this.eLetAccIn(acc, main, pg);
	}

	// private static boolean UseInlineChoice = false;
	//
	// private NezCC2.Expression emitCombiChoice(int acc, Expression e, NezCC2
	// pg) {
	// if (UseInlineChoice && pg.useLambda()) {
	// NezCC2.Expression innerFunc = this.getInFunc(e.get(e.size() - 1), pg);
	// if (innerFunc != null) {
	// NezCC2.Expression second = innerFunc;
	// for (int i = e.size() - 2; i >= 0; i--) {
	// NezCC2.Expression first = this.getInFunc(e.get(i), pg);
	// innerFunc = this.emitChoiceFunc2(pg, acc, first, second);
	// second = pg.emitParserLambda(innerFunc);
	// }
	// return innerFunc;
	// }
	// }
	// return null;
	// }

	@Override
	public NezCC2.Expression visitDispatch(PDispatch e, NezCC2 pg) {
		List<NezCC2.Expression> exprs = new ArrayList<>(e.size() + 1);
		exprs.add(this.eFail(pg));
		if (this.isAllConsumed(e)) {
			for (int i = 0; i < e.size(); i++) {
				Expression sub = e.get(i);
				if (sub instanceof PPair) {
					assert (!(sub.get(0) instanceof PPair));
					exprs.add(this.match(sub.get(1), pg));
				} else {
					exprs.add(this.eSucc(pg));
				}
			}
			return pg.emitDispatch(pg.emitJumpIndex(e.indexMap, true), exprs);
		} else {
			for (int i = 0; i < e.size(); i++) {
				exprs.add(this.patch(e.get(i), pg));
			}
			return pg.emitDispatch(pg.emitJumpIndex(e.indexMap, false), exprs);
		}
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
				return pg.p("mnext(px) && $0", this.match(e.get(1), pg));
			}
		}
		return this.match(e, pg);
	}

	@Override
	public NezCC2.Expression visitOption(POption e, NezCC2 pg) {
		int acc = this.varStacks(POS, e.get(0));
		NezCC2.Expression inline = this.emitCombiOption(acc, e, pg);
		if (inline == null) {
			return this.emitOption(pg, acc, this.match(e.get(0), pg));
		}
		return inline;
	}

	private NezCC2.Expression emitOption(NezCC2 pg, int acc, NezCC2.Expression first) {
		NezCC2.Expression second = this.mback(pg, acc);
		return this.eLetAccIn(acc, pg.p("$0 || $1", first, second), pg);
	}

	NezCC2.Expression emitCombiOption(int acc, Expression e, NezCC2 pg) {
		NezCC2.Expression lambda = this.getInFunc(e.get(0), pg);
		if (lambda != null) {
			String func = this.funcAcc("option", acc);
			return pg.apply(func, "px", lambda);
		}
		return null;
	}

	@Override
	public NezCC2.Expression visitMany(PMany e, NezCC2 pg) {
		int acc = this.varStacks(POS, e.get(0));
		if (e.isOneMore()) {
			acc |= CNT;
		}
		NezCC2.Expression inline = this.emitCombiMany(acc, e, pg);
		if (inline == null) {
			NezCC2.Expression cond = this.match(e.get(0), pg);
			return this.emitMany(pg, acc, cond);
		}
		return inline;
	}

	private NezCC2.Expression emitMany(NezCC2 pg, int acc, NezCC2.Expression cond) {
		if ((acc & EMPTY) == EMPTY) {
			cond = pg.p("$0 && pos < px.pos", cond);
		}
		// if (!pg.isDefined("while")) {
		NezCC2.Expression main = pg.ifexpr(cond, pg.apply(this.getCurrentFuncName(), "px"), this.mback(pg, acc));
		return this.eLetAccIn(acc, main, pg);
		// } else {
		// B block = pg.beginBlock();
		// pg.emitWhileStmt(block, cond, () -> this.emitUpdate(pg, acc));
		// pg.emitStmt(block, pg.emitReturn(back));
		// return this.emitVarDecl(pg, acc, true, pg.endBlock(block));
		// }
	}

	private NezCC2.Expression emitCombiMany(int acc, Expression e, NezCC2 pg) {
		NezCC2.Expression lambda = this.getInFunc(e.get(0), pg);
		if (lambda != null) {
			if (!NonEmpty.isAlwaysConsumed(e.get(0))) {
				acc |= EMPTY;
			}
			String func = this.funcAcc("many", acc);
			if (!pg.isDefined("while") && (acc & CNT) == CNT) {
				return pg.apply(func, "px", lambda, 0);
			} else {
				return pg.apply(func, "px", lambda);
			}
		}
		return null;
	}

	@Override
	public NezCC2.Expression visitAnd(PAnd e, NezCC2 pg) {
		NezCC2.Expression inline = this.emitCombiAnd(POS, e, pg);
		if (inline == null) {
			return this.emitAnd(pg, POS, this.match(e.get(0), pg));
		}
		return inline;
	}

	private NezCC2.Expression emitAnd(NezCC2 pg, int acc, NezCC2.Expression inner) {
		NezCC2.Expression main = pg.p("$0 && $1", inner, this.mback(pg, POS));
		return this.eLetAccIn(POS, main, pg);
	}

	private NezCC2.Expression emitCombiAnd(int acc, Expression e, NezCC2 pg) {
		Expression p = Expression.deref(e.get(0));
		if (p instanceof PAny) {
			return pg.apply("neof", "px");
		}
		// ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		// if (bs != null) {
		// this.funcAcc("getbyte");
		// return pg.emitMatchByteSet(bs, null, false);
		// }
		NezCC2.Expression lambda = this.getInFunc(e.get(0), pg);
		if (lambda != null) {
			String func = this.funcAcc("and", POS);
			return pg.apply(func, "px", lambda);
		}
		return null;
	}

	@Override
	public NezCC2.Expression visitNot(PNot e, NezCC2 pg) {
		int acc = this.varStacks(POS, e.get(0));
		NezCC2.Expression inline = this.emitCombiNot(acc, e, pg);
		if (inline == null) {
			return this.emitNot(pg, acc, this.match(e.get(0), pg));
		}
		return inline;
	}

	private NezCC2.Expression emitNot(NezCC2 pg, int acc, NezCC2.Expression inner) {
		// NezCC2.Expression main = pg.emitIfB(inner, pg.emitFail(), this.mback(pg,
		// acc));
		NezCC2.Expression main = pg.p("!$0 && $1", inner, this.mback(pg, acc));
		return this.eLetAccIn(acc, main, pg);
	}

	private NezCC2.Expression emitCombiNot(int acc, Expression e, NezCC2 pg) {
		Expression p = Expression.deref(e.get(0));
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
		NezCC2.Expression lambda = this.getInFunc(e.get(0), pg);
		if (lambda != null) {
			String func = this.funcAcc("not", acc);
			return pg.apply(func, "px", lambda);
		}
		return null;
	}

	private NezCC2.Expression getInFunc(Expression inner, NezCC2 pg) {
		if (pg.isDefined("lambda") && this.isInline(inner, pg)) {
			NezCC2.Expression e = this.match(inner, pg, false);
			return pg.apply(null, "px", e);
			// String px = this.V("px");
			// String lambda = String.format(this.s("lambda"), px, inner);
			// String p = "p" + this.varSuffix();
			// return lambda.replace("px", p);
		} else {
			this.match(inner, pg, true);
			return pg.p("^" + this.getFuncName(inner));
		}
	}

	@Override
	public NezCC2.Expression visitTree(PTree e, NezCC2 pg) {
		NezCC2.Expression pe = pg.emitAnd(this.match(e.get(0), pg), pg.endTree(e.endShift, e.tag, e.value));
		if (e.folding) {
			return pg.emitAnd(pg.foldTree(e.beginShift, e.label), pe);
		} else {

			return pg.emitAnd(pg.beginTree(e.beginShift), pe);
		}
	}

	@Override
	public NezCC2.Expression visitDetree(PDetree e, NezCC2 pg) {
		NezCC2.Expression inline = this.emitCombiDetree(e, pg);
		if (inline == null) {
			NezCC2.Expression main = pg.p("$0 && $1", this.match(e.get(0), pg), this.mback(pg, TREE));
			return this.eLetAccIn(TREE, main, pg);
		}
		return inline;
	}

	private NezCC2.Expression emitCombiDetree(PDetree e, NezCC2 pg) {
		NezCC2.Expression lambda = this.getInFunc(e.get(0), pg);
		if (lambda != null) {
			String funcName = this.funcAcc("detree", TREE);
			return pg.apply(funcName, "px", lambda);
		}
		return null;
	}

	@Override
	public NezCC2.Expression visitLinkTree(PLinkTree e, NezCC2 pg) {
		NezCC2.Expression inline = this.emitCombiLink(e, pg);
		if (inline == null) {
			NezCC2.Expression main = pg.p("$0 && backLink(px, $1)", this.match(e.get(0), pg), e.label);
			return this.eLetAccIn(TREE, main, pg);
		}
		return inline;
	}

	private NezCC2.Expression emitCombiLink(PLinkTree e, NezCC2 pg) {
		NezCC2.Expression lambda = this.getInFunc(e.get(0), pg);
		if (lambda != null) {
			String funcName = this.funcAcc("link", TREE);
			return pg.apply(funcName, "px", e.label, lambda);
		}
		return null;
	}

	@Override
	public NezCC2.Expression visitTag(PTag e, NezCC2 pg) {
		// FIXME
		return pg.apply("tagtree", e.tag);
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

	// // ---
	// private final static String RuntimeLibrary = null;

	// protected void loadCombinator(NezCC2 pg) {
	// pg.defineLib2("back", (Object thunk) -> {
	// if (pg.isDefined("backpos")) {
	// this.funcAcc( "backpos");
	// }
	// int acc = (Integer) thunk;
	// String funcName = "back" + acc;
	// pg.defineLib(funcName, () -> {
	// String[] args = pg.getStackNames(acc);
	// pg.declFunc(0, pg.T("matched"), funcName, pg.joins("px", args), () -> {
	// B block = pg.beginBlock();
	// pg.emitBack2(block, args);
	// pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
	// return pg.endBlock(block);
	// });
	// });
	// });
	//
	// pg.defineLib2("choice", (Object thunk) -> {
	// int acc = (Integer) thunk;
	// String funcName = "choice" + acc;
	// pg.defineLib(funcName, () -> {
	// this.funcAcc( "back", acc & ~(CNT | EMPTY));
	// this.funcAcc( "ParserFunc");
	// pg.declFunc(pg.T("matched"), funcName, "px", "f", "f2", () -> {
	// NezCC2.Expression first = pg.emitApply(pg.V("f"), "px");
	// NezCC2.Expression second = pg.emitAnd(this.mback(pg, acc),
	// pg.emitApply(pg.V("f2"), "px"));
	// NezCC2.Expression result = this.emitVarDecl(pg, acc, false,
	// pg.emitReturn(pg.emitOr(first,
	// second)));
	// return result;
	// });
	// });
	// });
	//
	// pg.defineLib2("many", (Object thunk) -> {
	// int acc = (Integer) thunk;
	// String funcName = "many" + acc;
	// pg.defineLib(funcName, () -> {
	// this.funcAcc( "back", acc & ~(CNT | EMPTY));
	// this.funcAcc( "ParserFunc");
	// if (!pg.isDefined("while") && (acc & CNT) == CNT) {
	// pg.declFunc(pg.T("matched"), funcName, "px", "f", "cnt", () -> {
	// return (this.emitMany(pg, funcName, acc, pg.emitApply(pg.V("f"),
	// "px")));
	// });
	// } else {
	// pg.declFunc(pg.T("matched"), funcName, "px", "f", () -> {
	// return (this.emitMany(pg, funcName, acc, pg.emitApply(pg.V("f"),
	// "px")));
	// });
	// }
	// });
	// });
	//
	// pg.defineLib2("option", (Object thunk) -> {
	// int acc = (Integer) thunk;
	// String funcName = "option" + acc;
	// pg.defineLib(funcName, () -> {
	// this.funcAcc( "back", acc & ~(CNT | EMPTY));
	// this.funcAcc( "ParserFunc");
	// pg.declFunc(pg.T("matched"), funcName, "px", "f", () -> {
	// return (this.emitOption(pg, acc, pg.emitApply(pg.V("f"), "px")));
	// });
	// });
	// });
	//
	// pg.defineLib("and" + POS, () -> {
	// this.funcAcc( "back", POS);
	// this.funcAcc( "ParserFunc");
	// pg.declFunc(pg.T("matched"), "and" + POS, "px", "f", () -> {
	// return (this.emitAnd(pg, POS, pg.emitApply(pg.V("f"), "px")));
	// });
	// });
	//
	// pg.defineLib2("not", (Object thunk) -> {
	// int acc = (Integer) thunk;
	// String funcName = "not" + acc;
	// pg.defineLib(funcName, () -> {
	// this.funcAcc( "back", acc & ~(CNT | EMPTY));
	// this.funcAcc( "ParserFunc");
	// pg.declFunc(pg.T("matched"), funcName, "px", "f", () -> {
	// return (this.emitNot(pg, acc, pg.emitApply(pg.V("f"), "px")));
	// });
	// });
	// });
	//
	// pg.defineLib("detree" + TREE, () -> {
	// this.funcAcc( "back", TREE);
	// this.funcAcc( "ParserFunc");
	// pg.declFunc(pg.T("matched"), "detree" + TREE, "px", "f", () -> {
	// NezCC2.Expression main = pg.emitAnd(pg.emitApply(pg.V("f"), "px"),
	// this.mback(pg, TREE));
	// return this.emitVarDecl(pg, TREE, false, main);
	// });
	// });
	//
	// pg.defineLib("link" + TREE, () -> {
	// this.funcAcc( "backLink");
	// this.funcAcc( "ParserFunc");
	// pg.declFunc(pg.T("matched"), "link" + TREE, "px", "nlabel", "f", () -> {
	// NezCC2.Expression main = pg.emitAnd(pg.emitApply(pg.V("f"), "px"),
	// pg.backLink(pg.V("nlabel")));
	// return this.emitVarDecl(pg, TREE, false, main);
	// });
	// });
	//
	// }

	//

	private int u = 0;

	// final static int POS = 1;
	// final static int TREE = 1 << 1;
	// final static int STATE = 1 << 2;
	// final static int CNT = 1 << 3;
	// final static int EMPTY = 1 << 4;

	private int varStacks(int acc, Expression e) {
		if (Typestate.compute(e) != Typestate.Unit) {
			acc |= TREE;
		}
		if (Stateful.isStateful(e)) {
			acc |= STATE;
		}
		return acc;
	}

	private String funcAcc(String f, int acc) {
		return f + acc;
	}

	private NezCC2.Expression eLetAccIn(int acc, NezCC2.Expression e, NezCC2 pg) {
		if ((acc & CNT) == CNT) {
			e = pg.p("let state = 0; $0", e);
		}
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
		if ((acc & CNT) == CNT) {
			l.add("cnt");
		}
		return l.toArray(new String[l.size()]);
	}

	private NezCC2.Expression mback(NezCC2 pg, int acc) {
		String funcName = "back" + (acc & ~(CNT | EMPTY));
		String[] args = this.accNames(acc & ~(CNT | EMPTY), "px");
		return pg.apply(funcName, args);
	}

	// function
	HashMap<String, String> exprFuncMap = new HashMap<>();

	private String getFuncName(Expression e) {
		if (e instanceof PNonTerminal) {
			String uname = ((PNonTerminal) e).getUniqueName();
			// if (uname.indexOf('"') > 0) {
			// String funcName = this.symbolMap.get(uname);
			// if (funcName == null) {
			// funcName = "t" + this.symbolMap.size();
			// this.symbolMap.put(uname, funcName);
			// }
			// return funcName;
			// }
			return uname.replace(':', '_').replace('.', '_').replace('&', '_');
		}
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
								ParserGeneratorVisitor2.this.crossRefNames.add(nextNode);
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
		OConsole.println(line, args);
	}

}