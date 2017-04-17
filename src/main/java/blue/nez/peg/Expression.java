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

package blue.nez.peg;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import blue.nez.ast.SourcePosition;
import blue.nez.ast.Symbol;
import blue.nez.peg.expression.ByteSet;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.origami.util.OStringUtils;
import blue.origami.util.StringCombinator;

public abstract class Expression extends AbstractList<Expression> implements StringCombinator {

	public abstract <V, A> V visit(ExpressionVisitor<V, A> v, A a);

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

	public Expression desugar() {
		return this;
	}

	/* External reference */

	private Object ref = null;

	// protected Expression(Object ref) {
	// this.ref = ref;
	// }

	public final Object getRef() {
		return this.ref;
	}

	public final Expression setRef(Object ref) {
		this.ref = ref;
		return this;
	}

	/* source location */

	public final SourcePosition getSourcePosition() {
		if (this.ref instanceof SourcePosition) {
			return (SourcePosition) this.ref;
		}
		return null;
	}

	public final Expression setSourcePosition(SourcePosition s) {
		if (!(this.ref instanceof SourcePosition)) {
			this.ref = s;
		}
		return this;
	}

	// formal expressions

	/* Unary */

	public final static boolean isString(Expression e) {
		if (e instanceof PPair) {
			Expression left = ((PPair) e).left;
			if (left instanceof PByte) {
				return isString(((PPair) e).right);
			}
			return false;
		}
		if (e instanceof PByte) {
			return true;
		}
		return false;
	}

	public final static Expression extractString(Expression e, List<java.lang.Integer> l) {
		if (e instanceof PPair) {
			Expression left = ((PPair) e).left;
			if (left instanceof PByte) {
				if (l != null) {
					l.add(((PByte) left).byteChar());
				}
				return extractString(((PPair) e).right, l);
			}
			return e;
		}
		if (e instanceof PByte) {
			if (l != null) {
				l.add(((PByte) e).byteChar());
			}
			return Expression.defaultEmpty;
		}
		return e;
	}

	/* AST */

	public final static void formatReplace(String value, StringBuilder sb) {
		// value;
		sb.append('`');
		for (int i = 0; i < value.length(); i++) {
			int c = value.charAt(i) & 0xff;
			ByteSet.formatByte(c, "`", "\\x02x", sb);
		}
		sb.append('`');
	}

	/* Function */
	public static PEmpty defaultEmpty = new PEmpty();
	public static PFail defaultFailure = new PFail();
	public static PAny defaultAny = new PAny();

	final static Symbol symbolTableName(PNonTerminal n, Symbol table) {
		if (table == null) {
			String u = n.getLocalName().replace("~", "");
			return Symbol.unique(u);
		}
		return table;
	}

	// utils

	public final static Expression newRange(int c, int c2) {
		if (c == c2) {
			return new PByte(c);
		} else {
			PByteSet b = new PByteSet();
			b.set(c, c2, true);
			return b;
		}
	}

	public final static List<Expression> newList(int size) {
		return new ArrayList<>(size);
	}

	public final static void addSequence(List<Expression> l, Expression e) {
		if (e instanceof PEmpty) {
			return;
		}
		if (e instanceof PPair) {
			((PPair) e).expand(l);
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.get(l.size() - 1);
			if (prev instanceof PFail) {
				return;
			}
		}
		l.add(e);
	}

	public final static void addChoice(List<Expression> l, Expression e) {
		if (e instanceof PChoice) {
			for (int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if (e instanceof PFail) {
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.get(l.size() - 1);
			if (prev instanceof PEmpty) {
				return;
			}
		}
		l.add(e);
	}

	/**
	 * Creates a pair expression from a list of expressions
	 * 
	 * @param l
	 *            a list of expressions
	 * @return
	 */

	public final static Expression newSequence(List<Expression> l) {
		if (l.size() == 0) {
			return defaultEmpty;
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		return newSequence(l, 0, l.size());
	}

	private final static Expression newSequence(List<Expression> l, int start, int end) {
		Expression first = l.get(start);
		if (start + 1 == end) {
			return first;
		}
		return newSequence(first, newSequence(l, start + 1, end));
	}

	public final static Expression newChoice(List<Expression> l) {
		int size = l.size();
		boolean allCharacters = true;
		for (int i = 0; i < size; i++) {
			Expression e = l.get(i);
			if (e instanceof PEmpty) {
				size = i + 1;
				allCharacters = false;
				break;
			}
			if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny) {
				continue;
			}
			allCharacters = false;
		}
		if (size == 1) {
			return l.get(0);
		}
		if (allCharacters) {
			PByteSet b = new PByteSet();
			for (int i = 0; i < size; i++) {
				Expression e = l.get(i);
				if (e instanceof PAny) {
					return e;
				}
				if (e instanceof PByteSet) {
					b.union((PByteSet) e);
				} else {
					b.set(((PByte) e).byteChar(), true);
				}
			}
			return b;
		}
		Expression[] inners = new Expression[size];
		for (int i = 0; i < size; i++) {
			inners[i] = l.get(i);
		}
		return new PChoice(inners);
	}

	public final static Expression newSequence(Expression first, Expression second) {
		if (first instanceof PFail) {
			return first;
		}
		if (second instanceof PFail) {
			return second;
		}
		if (second instanceof PEmpty) {
			return first;
		}
		if (first instanceof PEmpty) {
			return second;
		}
		return new PPair(first, second).desugar();
	}

	public final static Expression append(Expression e1, Expression e2) {
		if (e1 instanceof PPair) {
			List<Expression> l = Expression.newList(64);
			((PPair) e1).expand(l);
			addSequence(l, e2);
			return newSequence(l);
		}
		return newSequence(e1, e2);
	}

	public final static Expression newString(byte[] utf8) {
		if (utf8.length == 0) {
			return defaultEmpty;
		}
		if (utf8.length == 1) {
			return new PByte(utf8[0] & 0xff);
		}
		List<Expression> l = newList(utf8.length);
		for (int i = 0; i < utf8.length; i++) {
			l.add(new PByte(utf8[i]));
		}
		return newSequence(l);
	}

	public final static Expression newString(String text) {
		return newString(OStringUtils.utf8(text));
	}

	public final static Expression newTree(Expression e) {
		return new PTree(false, null, 0, e, null, null, 0);
	}

	public final static Expression newFoldTree(Symbol label, Expression e) {
		return new PTree(true, label, 0, e, null, null, 0);
	}

	public final static Expression deref(Expression e) {
		while (e instanceof PNonTerminal) {
			PNonTerminal nterm = (PNonTerminal) e;
			e = nterm.getProduction().getExpression();
		}
		return e;
	}

	// Visitor

	public static abstract class AbstractExpressionVisitor<A> extends ExpressionVisitor<Expression, A> {
		protected final Grammar base;

		public AbstractExpressionVisitor(Grammar base) {
			this.base = base;
		}

		protected boolean enableExternalDuplication = true; // FIXME: false

		protected Object ref(Expression e) {
			if (this.enableExternalDuplication) {
				return e.getRef();
			}
			return null;
		}

	}

	public static class Duplicator<A> extends AbstractExpressionVisitor<A> {

		public Duplicator(Grammar grammar) {
			super(grammar);
		}

		protected boolean enableFullDuplication = false;

		protected Expression dup(Expression e, A a) {
			return e.visit(this, a);
		}

		protected Expression dup(Expression e, int i, A a) {
			return e.get(i).visit(this, a);
		}

		@Override
		public Expression visitNonTerminal(PNonTerminal e, A a) {
			if (e.getGrammar() != this.base && this.base != null) {
				String lname = e.getLocalName();
				return new PNonTerminal(this.base, e.getNameSpace(), lname);
			}
			if (this.enableFullDuplication) {
				return new PNonTerminal(e.getGrammar(), e.getLocalName());
			}
			return e;
		}

		@Override
		public Expression visitEmpty(PEmpty e, A a) {
			if (this.enableFullDuplication) {
				return new PEmpty();
			}
			return e;
		}

		@Override
		public Expression visitFail(PFail e, A a) {
			if (this.enableFullDuplication) {
				return new PFail();
			}
			return e;
		}

		@Override
		public Expression visitByte(PByte e, A a) {
			if (this.enableFullDuplication) {
				return new PByte(e.byteChar());
			}
			return e;
		}

		@Override
		public Expression visitByteSet(PByteSet e, A a) {
			if (this.enableFullDuplication) {
				return new PByteSet(e.byteSet());
			}
			return e;
		}

		@Override
		public Expression visitAny(PAny e, A a) {
			if (this.enableFullDuplication) {
				return new PAny();
			}
			return e;
		}

		@Override
		public Expression visitPair(PPair e, A a) {
			Expression e0 = this.dup(e, 0, a);
			Expression e1 = this.dup(e, 1, a);
			if (e0 != e.get(0) || e1 != e.get(1) || this.enableFullDuplication) {
				return Expression.newSequence(e0, e1);
			}
			return e;
		}

		@Override
		public Expression visitChoice(PChoice e, A a) {
			boolean isModified = false;
			Expression[] en = new Expression[e.size()];
			for (int i = 0; i < e.size(); i++) {
				en[i] = this.dup(e, i, a);
				if (en[i] != e.get(i)) {
					isModified = true;
				}
			}
			if (isModified || this.enableFullDuplication) {
				return new PChoice(en);
			}
			return e;
		}

		@Override
		public Expression visitDispatch(PDispatch e, A a) {
			boolean isModified = false;
			Expression[] en = new Expression[e.size()];
			for (int i = 0; i < e.size(); i++) {
				en[i] = this.dup(e, i, a);
				if (en[i] != e.get(i)) {
					isModified = true;
				}
			}
			if (isModified || this.enableFullDuplication) {
				return new PDispatch(en, e.indexMap);
			}
			return e;
		}

		@Override
		public Expression visitOption(POption e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new POption(e0);
			}
			return e;
		}

		@Override
		public Expression visitRepetition(PRepetition e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PRepetition(e0, e.min, e.max);
			}
			return e;
		}

		@Override
		public Expression visitAnd(PAnd e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PAnd(e0);
			}
			return e;
		}

		@Override
		public Expression visitNot(PNot e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PNot(e0);
			}
			return e;
		}

		@Override
		public Expression visitTree(PTree e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PTree(e.folding, e.label, e.beginShift, e0, e.tag, e.value, e.endShift);
			}
			return e;
		}

		@Override
		public Expression visitLinkTree(PLinkTree e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PLinkTree(e.label, e0);
			}
			return e;
		}

		@Override
		public Expression visitTag(PTag e, A a) {
			if (this.enableFullDuplication) {
				return new PTag(e.tag);
			}
			return e;
		}

		@Override
		public Expression visitReplace(PReplace e, A a) {
			if (this.enableFullDuplication) {
				return new PReplace(e.value);
			}
			return e;
		}

		@Override
		public Expression visitDetree(PDetree e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PDetree(e0);
			}
			return e;
		}

		@Override
		public Expression visitSymbolScope(PSymbolScope e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PSymbolScope(e.label, e0);
			}
			return e;
		}

		@Override
		public Expression visitSymbolAction(PSymbolAction e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PSymbolAction(e.action, e.label, (PNonTerminal) e0);
			}
			return e;
		}

		@Override
		public Expression visitSymbolPredicate(PSymbolPredicate e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PSymbolPredicate(e.pred, e.label, (PNonTerminal) e0, e.option);
			}
			return e;
		}

		@Override
		public Expression visitScan(PScan e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PScan(e.pattern, e0);
			}
			return e;
		}

		@Override
		public Expression visitRepeat(PRepeat e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new PRepeat(e0);
			}
			return e;
		}

		@Override
		public Expression visitIf(PIfCondition e, A a) {
			if (this.enableFullDuplication) {
				return new PIfCondition(e.nflag);
			}
			return e;
		}

		@Override
		public Expression visitOn(POnCondition e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new POnCondition(e.nflag, e0);
			}
			return e;
		}

		@Override
		public Expression visitTrap(PTrap e, A a) {
			if (this.enableFullDuplication) {
				return new PTrap(e.trapid, e.uid);
			}
			return e;
		}

	}

	public static class Rewriter<A> extends ExpressionVisitor<Expression, A> {

		protected Expression rewrite(Expression e, A a) {
			return e.visit(this, a);
		}

		protected Expression rewrite(Expression e, int index, A a) {
			return e.get(index).visit(this, a);
		}

		@Override
		public Expression visitNonTerminal(PNonTerminal e, A a) {
			return e;
		}

		@Override
		public Expression visitEmpty(PEmpty e, A a) {
			return e;
		}

		@Override
		public Expression visitFail(PFail e, A a) {
			return e;
		}

		@Override
		public Expression visitByte(PByte e, A a) {
			return e;
		}

		@Override
		public Expression visitByteSet(PByteSet e, A a) {
			return e;
		}

		@Override
		public Expression visitAny(PAny e, A a) {
			return e;
		}

		@Override
		public Expression visitPair(PPair e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			e.set(1, this.rewrite(e, 1, a));
			return e.desugar();
		}

		@Override
		public Expression visitChoice(PChoice e, A a) {
			for (int i = 0; i < e.size(); i++) {
				e.set(i, this.rewrite(e, i, a));
			}
			return e;
		}

		@Override
		public Expression visitOption(POption e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitRepetition(PRepetition e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitAnd(PAnd e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitNot(PNot e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitDetree(PDetree e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitTree(PTree e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitLinkTree(PLinkTree e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitTag(PTag e, A a) {
			return e;
		}

		@Override
		public Expression visitReplace(PReplace e, A a) {
			return e;
		}

		@Override
		public Expression visitSymbolScope(PSymbolScope e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitSymbolAction(PSymbolAction e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitSymbolPredicate(PSymbolPredicate e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitScan(PScan e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitRepeat(PRepeat e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitIf(PIfCondition e, A a) {
			return e;
		}

		@Override
		public Expression visitOn(POnCondition e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitTrap(PTrap e, A a) {
			return e;
		}

		@Override
		public Expression visitDispatch(PDispatch e, A a) {
			for (int i = 1; i < e.inners.length; i++) {
				e.inners[i] = this.rewrite(e, i, a);
			}
			return e;
		}
	}

}
