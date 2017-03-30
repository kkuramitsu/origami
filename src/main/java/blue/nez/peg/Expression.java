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
import blue.origami.util.OStringUtils;
import blue.origami.util.StringCombinator;

public abstract class Expression extends AbstractList<Expression> implements StringCombinator {

	public abstract <VAL, ARG> VAL visit(ExpressionVisitor<VAL, ARG> v, ARG a);

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

	public Expression desugar() {
		return this;
	}

	/* External reference */

	private Object ref = null;

	protected Expression(Object ref) {
		this.ref = ref;
	}

	public final Object getExternalReference() {
		return this.ref;
	}

	public final void setExternalReference(Object ref) {
		this.ref = ref;
	}

	/* source location */

	public final SourcePosition getSourceLocation() {
		if (this.ref instanceof SourcePosition) {
			return (SourcePosition) this.ref;
		}
		return null;
	}

	public final void setSourceLocation(SourcePosition s) {
		this.ref = s;
	}

	public final void setSourceLocation(Expression e) {
		this.ref = e.getExternalReference();
	}

	// public String formatSourceMessage(String type, String msg) {
	// SourcePosition s = getSourceLocation();
	// if (s != null) {
	// return s.formatSourceMessage(type, msg);
	// }
	// return "(" + type + ") " + msg;
	// }

	// term, unary, binary, array

	static abstract class PTerm extends Expression {
		protected PTerm(Object ref) {
			super(ref);
		}

		@Override
		public final int size() {
			return 0;
		}

		@Override
		public final Expression get(int index) {
			return null;
		}
	}

	public static abstract class PUnary extends Expression {
		protected Expression inner;

		protected PUnary(Expression inner, Object ref) {
			super(ref);
			this.inner = inner;
		}

		@Override
		public final int size() {
			return 1;
		}

		@Override
		public final Expression get(int index) {
			return this.inner;
		}

		@Override
		public final Expression set(int index, Expression e) {
			Expression old = this.inner;
			this.inner = e;
			return old;
		}

		protected final void formatUnary(String prefix, String suffix, StringBuilder sb) {
			if (prefix != null) {
				sb.append(prefix);
			}
			String pre = "(";
			String post = ")";
			if (this.inner instanceof PNonTerminal || this.inner instanceof PTerm || this.inner instanceof PTree
					|| Expression.isString(this.inner)) {
				pre = "";
				post = "";
			}
			sb.append(pre);
			this.get(0).strOut(sb);
			sb.append(post);
			if (suffix != null) {
				sb.append(suffix);
			}
		}
	}

	static abstract class PBinary extends Expression {
		public Expression left;
		public Expression right;

		protected PBinary(Expression left, Expression right, Object ref) {
			super(ref);
			this.left = left;
			this.right = right;
		}

		protected PBinary(Expression left, Expression right) {
			super(null);
			this.left = left;
			this.right = right;
		}

		@Override
		public final int size() {
			return 2;
		}

		@Override
		public final Expression get(int index) {
			assert (index < 2);
			return index == 0 ? this.left : this.right;
		}

		@Override
		public final Expression set(int index, Expression e) {
			assert (index < 2);
			if (index == 0) {
				Expression p = this.left;
				this.left = e;
				return p;
			} else {
				Expression p = this.right;
				this.right = e;
				return p;
			}
		}

		public void formatPair(String delim, StringBuilder sb) {
			if (this.left instanceof Expression.PChoice) {
				sb.append("(");
				this.left.strOut(sb);
				sb.append(")");
			} else {
				this.left.strOut(sb);
			}
			sb.append(delim);
			if (this.right instanceof Expression.PChoice) {
				sb.append("(");
				this.right.strOut(sb);
				sb.append(")");
			} else {
				this.right.strOut(sb);
			}
		}

	}

	abstract static class PArray extends Expression {
		public Expression[] inners;

		protected PArray(Expression[] inners, Object ref) {
			super(ref);
			this.inners = inners;
		}

		@Override
		public final int size() {
			return this.inners.length;
		}

		@Override
		public final Expression get(int index) {
			return this.inners[index];
		}

		@Override
		public Expression set(int index, Expression e) {
			Expression oldExpresion = this.inners[index];
			this.inners[index] = e;
			return oldExpresion;
		}

		protected final boolean equalsList(Expression.PArray l) {
			if (this.size() == l.size()) {
				for (int i = 0; i < this.size(); i++) {
					if (!this.get(i).equals(l.get(i))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		protected final void formatList(String delim, StringBuilder sb) {
			for (int i = 0; i < this.size(); i++) {
				if (i > 0) {
					sb.append(delim);
				}
				Expression inner = this.get(i);
				if (inner instanceof Expression.PChoice) {
					sb.append("(");
					inner.strOut(sb);
					sb.append(")");
				} else {
					inner.strOut(sb);
				}
			}
		}
	}

	// formal expressions

	public final static class PNonTerminal extends Expression {
		// public boolean isLeftRecursion;
		private final Grammar grammar;
		private final String ns;
		private final String name;

		public PNonTerminal(Grammar g, String ns, String name, Object ref) {
			super(ref);
			// this.isLeftRecursion = isLeftRecursion;
			this.grammar = g;
			this.ns = ns;
			this.name = name;
		}

		public PNonTerminal(Grammar g, String pname, Object ref) {
			this(g, null, pname, ref);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof PNonTerminal) {
				return this.name.equals(((PNonTerminal) o).name) && this.grammar == ((PNonTerminal) o).getGrammar();
			}
			return false;
		}

		public final Grammar getGrammar() {
			if (this.ns != null) {
				return this.grammar.getGrammar(this.ns);
			}
			return this.grammar;
		}

		public final String getLocalName() {
			return this.name;
		}

		public final String getUniqueName() {
			return this.getGrammar().getUniqueName(this.name);
		}

		public final Expression getExpression() {
			return this.getGrammar().getExpression(this.name);
		}

		public final Production getProduction() {
			return this.getGrammar().getProduction(this.name);
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public Expression get(int index) {
			return null;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitNonTerminal(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			if (this.ns != null) {
				sb.append(this.ns);
				sb.append("::");
			}
			sb.append(this.name);
		}
	}

	public final static void quote(String text, StringBuilder sb) {
		sb.append('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			String s = null;
			switch (c) {
			case '\"':
				s = "\"";
				break;
			case '\n':
				s = "\\n";
				break;
			case '\t':
				s = "\\t";
				break;
			default:
				s = String.valueOf(c);
			}
			sb.append(s);
		}
		sb.append('"');
	}

	/**
	 * The Expression.Empty represents an empty expression, denoted '' in
	 * Expression.
	 * 
	 * @author kiki
	 *
	 */

	public static class PEmpty extends PTerm {
		PEmpty() {
			super(null);
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Expression.PEmpty);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitEmpty(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("''");
		}
	}

	/**
	 * The Expression.Fail represents a failure expression, denoted !'' in
	 * Expression.
	 * 
	 * @author kiki
	 *
	 */

	public static class PFail extends PTerm {
		PFail() {
			super(null);
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Expression.PFail);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitFail(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("!''");
		}

	}

	/**
	 * Expression.Byte represents a single-byte string literal, denoted as 'a'
	 * in Expression.
	 *
	 * @author kiki
	 *
	 */

	public static class PByte extends PTerm {
		/**
		 * byteChar
		 */
		public final int byteChar;

		public PByte(int byteChar, Object ref) {
			super(ref);
			this.byteChar = byteChar & 0xff;
		}

		public PByte(int byteChar) {
			this(byteChar, null);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PByte) {
				return this.byteChar == ((Expression.PByte) o).byteChar;
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitByte(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("'");
			Expression.formatByte(this.byteChar, "'", "0x%02x", sb);
			sb.append("'");
		}
	}

	public final static void formatByte(int byteChar, String escaped, String fmt, StringBuilder sb) {
		if (escaped.indexOf(byteChar) != -1) {
			sb.append("\\");
			sb.append((char) byteChar);
		}
		switch (byteChar) {
		case '\n':
			sb.append("\\n");
			return;
		case '\t':
			sb.append("\\t");
			return;
		case '\r':
			sb.append("\\r");
			return;
		case '\\':
			sb.append("\\\\");
			return;
		}
		if (Character.isISOControl(byteChar) || byteChar > 127) {
			sb.append(String.format(fmt/* "0x%02x" */, byteChar));
			return;
		}
		sb.append((char) byteChar);
	}

	/**
	 * Expression.Any represents an any character, denoted as . in Expression.
	 * 
	 * @author kiki
	 *
	 */

	public static class PAny extends PTerm {

		public PAny(Object ref) {
			super(ref);
		}

		public PAny() {
			this(null);
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof PAny);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitAny(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append(".");
		}

	}

	/**
	 * Expression.ByteSet is a bitmap-based representation of the character
	 * class [X-y]
	 * 
	 * @author kiki
	 *
	 */

	public static class PByteSet extends PTerm {
		protected int bits[];

		public final boolean is(int n) {
			return (this.bits[n / 32] & (1 << (n % 32))) != 0;
		}

		public final void set(int n, boolean b) {
			if (b) {
				int mask = 1 << (n % 32);
				this.bits[n / 32] |= mask;
			} else {
				int mask = ~(1 << (n % 32));
				this.bits[n / 32] &= mask;
			}
		}

		public final void set(int s, int e, boolean b) {
			for (int i = s; i <= e; i++) {
				this.set(i, b);
			}
		}

		public final void union(PByteSet b) {
			for (int i = 0; i < this.bits.length; i++) {
				this.bits[i] = this.bits[i] | b.bits[i];
			}
		}

		public final boolean[] byteSet() {
			boolean[] b = new boolean[256];
			for (int i = 0; i < 256; i++) {
				b[i] = this.is(i);
			}
			return b;
		}

		public final int n(int n) {
			return this.bits[n];
		}

		public PByteSet(Object ref) {
			super(ref);
			this.bits = new int[8];
		}

		PByteSet(int[] bits, Object ref) {
			this(ref);
			for (int i = 0; i < bits.length; i++) {
				this.bits[i] = bits[i];
			}
		}

		// public ByteSet(boolean[] b) {
		// this(b, null);
		// }

		@Override
		public final boolean equals(Object o) {
			if (o instanceof PByteSet) {
				PByteSet e = (PByteSet) o;
				for (int i = 0; i < this.bits.length; i++) {
					if (this.bits[i] != e.bits[i]) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitByteSet(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("[");
			int start = -1;
			for (int i = 0; i < 256; i++) {
				if (start == -1 && this.is(i)) {
					start = i;
					continue;
				}
				if (start != -1 && !this.is(i)) {
					this.format(start, i - 1, sb);
					start = -1;
					continue;
				}
			}
			if (start != -1) {
				this.format(start, 255, sb);
			}
			sb.append("]");
		}

		private void format(int start, int end, StringBuilder sb) {
			Expression.formatByte(start, "-]", "\\x%02x", sb);
			if (start != end) {
				sb.append("-");
				Expression.formatByte(end, "-]", "\\x%02x", sb);
			}
		}
	}

	/* Unary */

	/**
	 * Expression.Option represents an optional expression e?
	 * 
	 * @author kiki
	 *
	 */

	public static class POption extends Expression.PUnary {
		public POption(Expression e, Object ref) {
			super(e, ref);
		}

		public POption(Expression e) {
			super(e, null);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.POption) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitOption(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.formatUnary(null, "?", sb);
		}

	}

	/**
	 * Expression.Repetition is used to identify a common property of ZeroMore
	 * and OneMore expressions.
	 * 
	 * @author kiki
	 *
	 */

	public static class PRepetition extends PUnary {
		public final int min;
		public final int max;

		public final boolean isOneMore() {
			return this.min > 0;
		}

		public PRepetition(Expression e, int min, int max, Object ref) {
			super(e, ref);
			this.min = min;
			this.max = max;
		}

		public PRepetition(Expression e, int min, Object ref) {
			this(e, min, -1, ref);
		}

		public PRepetition(Expression e, int min, int max) {
			this(e, min, max, null);
		}

		public PRepetition(Expression e, int min) {
			this(e, min, -1, null);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Expression.PRepetition) {
				PRepetition r = (Expression.PRepetition) o;
				return (this.min == r.min && this.max == r.max && this.get(0).equals(r.get(0)));
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitRepetition(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.formatUnary(null, this.isOneMore() ? "+" : "*", sb);
		}

	}

	/**
	 * Expression.And represents an and-predicate &e.
	 * 
	 * @author kiki
	 *
	 */

	public static class PAnd extends PUnary {
		public PAnd(Expression e, Object ref) {
			super(e, ref);
		}

		public PAnd(Expression e) {
			super(e, null);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PAnd) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitAnd(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.formatUnary("&", null, sb);
		}

	}

	/**
	 * Expression.Not represents a not-predicate !e.
	 * 
	 * @author kiki
	 *
	 */

	public static class PNot extends PUnary {
		public PNot(Expression e, Object ref) {
			super(e, ref);
		}

		public PNot(Expression e) {
			super(e, null);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PNot) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitNot(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.formatUnary("!", null, sb);
		}

	}

	/**
	 * Expression.Pair is a pair representation of Expression.Sequence.
	 * 
	 * @author kiki
	 *
	 */

	/* OK (a (b c)) */
	/* NG ((a b) c) */

	public static class PPair extends PBinary {
		public PPair(Expression first, Expression next, Object ref) {
			super(first, next, ref);
		}

		public void expand(java.util.List<Expression> l) {
			if (this.left instanceof Expression.PPair) {
				((Expression.PPair) this.left).expand(l);
			} else {
				Expression.addSequence(l, this.left);
			}
			if (this.right instanceof Expression.PPair) {
				((Expression.PPair) this.right).expand(l);
			} else {
				Expression.addSequence(l, this.right);
			}
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PPair) {
				return this.get(0).equals(((Expression) o).get(0)) && this.get(1).equals(((Expression) o).get(1));
			}
			return false;
		}

		@Override
		public Expression desugar() {
			if (this.left instanceof Expression.PEmpty) {
				return this.right;
			}
			if (this.right instanceof Expression.PEmpty) {
				return this.left;
			}
			if (this.left instanceof Expression.PPair) {
				List<Expression> l = Expression.newList(64);
				Expression.addSequence(l, this.left);
				Expression.addSequence(l, this.right);
				return Expression.newSequence(l, this.getExternalReference());
			}
			return this;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitPair(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			if (this.left instanceof Expression.PByte) {
				List<Integer> l = new ArrayList<>();
				Expression lasting = extractString(this, l);
				if (l.size() > 1) {
					sb.append("'");
					for (int c : l) {
						Expression.formatByte(c, "'", "\\x%02x", sb);
					}
					sb.append("'");
					if (!(lasting instanceof PEmpty)) {
						sb.append(" ");
						lasting.strOut(sb);
					}
					return;
				}
			}
			this.formatPair(" ", sb);
		}
	}

	public final static boolean isString(Expression e) {
		if (e instanceof Expression.PPair) {
			Expression left = ((Expression.PPair) e).left;
			if (left instanceof Expression.PByte) {
				return isString(((Expression.PPair) e).right);
			}
			return false;
		}
		if (e instanceof Expression.PByte) {
			return true;
		}
		return false;
	}

	public final static Expression extractString(Expression e, List<java.lang.Integer> l) {
		if (e instanceof Expression.PPair) {
			Expression left = ((Expression.PPair) e).left;
			if (left instanceof Expression.PByte) {
				if (l != null) {
					l.add(((Expression.PByte) left).byteChar);
				}
				return extractString(((Expression.PPair) e).right, l);
			}
			return e;
		}
		if (e instanceof Expression.PByte) {
			if (l != null) {
				l.add(((Expression.PByte) e).byteChar);
			}
			return Expression.defaultEmpty;
		}
		return e;
	}

	/**
	 * Expression.Choice represents an ordered choice e / ... / e_n in
	 * Expression.
	 * 
	 * @author kiki
	 *
	 */

	public static class PChoice extends PArray {

		PChoice(Expression[] inners, Object ref) {
			super(inners, ref);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PChoice) {
				return this.equalsList((Expression.PArray) o);
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitChoice(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.formatList(" / ", sb);
		}
	}

	public static class PDispatch extends PArray {
		public final byte[] indexMap;

		public PDispatch(Expression[] inners, byte[] indexMap, Object ref) {
			super(inners, ref);
			this.indexMap = indexMap;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PDispatch) {
				Expression.PDispatch d = (Expression.PDispatch) o;
				if (d.indexMap.length != this.indexMap.length) {
					return false;
				}
				for (int i = 0; i < this.indexMap.length; i++) {
					if (this.indexMap[i] != d.indexMap[i]) {
						return false;
					}
				}
				return this.equalsList((Expression.PArray) o);
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitDispatch(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("<switch");
			for (int i = 0; i < this.size(); i++) {
				Expression.PByteSet bs = new Expression.PByteSet(null);
				for (int j = 0; j < this.indexMap.length; j++) {
					if ((this.indexMap[j] & 0xff) == i + 1) {
						bs.set(j, true);
					}
				}
				sb.append(" ");
				bs.strOut(sb);
				sb.append(":");
				this.get(i).strOut(sb);
			}
			sb.append(">");
		}
	}

	/* AST */

	public static class PTree extends PUnary {
		public int beginShift = 0;
		public int endShift = 0;
		public final boolean folding;
		public final Symbol label;
		public Symbol tag = null; // optimization parameter
		public String value = null; // optimization parameter

		public PTree(boolean folding, Symbol label, int beginShift, Expression inner, Symbol tag, String value,
				int endShift, Object ref) {
			super(inner, ref);
			this.folding = folding;
			this.label = label;
			this.beginShift = beginShift;
			this.endShift = endShift;
			this.tag = tag;
			this.value = value;
		}

		public PTree(Expression inner, Object ref) {
			this(false, null, 0, inner, null, null, 0, ref);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PTree) {
				Expression.PTree t = (Expression.PTree) o;
				if (t.beginShift != this.beginShift || t.endShift != this.endShift || t.tag != this.tag
						|| t.value != this.value) {
					return false;
				}
				return t.get(0).equals(this.get(0));
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitTree(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append('{');
			if (this.folding) {
				sb.append('$');
				if (this.label != null) {
					sb.append(this.label);
				}
				sb.append(" ");
			}
			this.get(0).strOut(sb);
			if (this.value != null) {
				sb.append(" ");
				formatReplace(this.value, sb);
			}
			if (this.tag != null) {
				sb.append(" #");
				sb.append(this.tag);
			}
			sb.append(" }");
		}
	}

	public static class PTag extends PTerm {
		public final Symbol tag;

		public final Symbol symbol() {
			return this.tag;
		}

		public PTag(Symbol tag, Object ref) {
			super(ref);
			this.tag = tag;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PTag) {
				return this.tag == ((Expression.PTag) o).tag;
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitTag(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("#" + this.tag);
		}
	}

	public static class PReplace extends PTerm {
		public String value;

		public PReplace(String value, Object ref) {
			super(ref);
			this.value = value;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PReplace) {
				return this.value.equals(((Expression.PReplace) o).value);
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitReplace(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			formatReplace(this.value, sb);
		}
	}

	public final static void formatReplace(String value, StringBuilder sb) {
		// value;
		sb.append('`');
		for (int i = 0; i < value.length(); i++) {
			int c = value.charAt(i) & 0xff;
			formatByte(c, "`", "\\x02x", sb);
		}
		sb.append('`');
	}

	public static class PLinkTree extends PUnary {
		public Symbol label;

		public PLinkTree(Symbol label, Expression e, Object ref) {
			super(e, ref);
			this.label = label;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PLinkTree && this.label == ((Expression.PLinkTree) o).label) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitLinkTree(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.formatUnary((this.label != null) ? "$" + this.label + "(" : "$(", ")", sb);
		}
	}

	public static class PDetree extends PUnary {
		public PDetree(Expression e, Object ref) {
			super(e, ref);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PDetree) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitDetree(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.formatUnary("~", null, sb);
		}

	}

	/* Function */
	public static Expression.PEmpty defaultEmpty = new Expression.PEmpty();
	public static Expression.PFail defaultFailure = new Expression.PFail();
	public static Expression.PAny defaultAny = new Expression.PAny(null);

	public static abstract class PFunction<T> extends PUnary {
		public final NezFunc funcName;
		public final T param;

		protected PFunction(NezFunc op, T param, Expression e, Object ref) {
			super(e == null ? defaultEmpty : e, ref);
			this.funcName = op;
			this.param = param;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof PFunction<?>) {
				PFunction<?> f = (PFunction<?>) o;
				return this.funcName == f.funcName && this.param.equals(f.param) && this.get(0).equals(f.get(0));
			}
			return false;
		}

		public boolean hasInnerExpression() {
			return this.get(0) != defaultEmpty;
		}

		/* function */
		@Override
		public void strOut(StringBuilder sb) {
			sb.append("<");
			sb.append(this.funcName);
			if (this.param != null) {
				sb.append(" ");
				sb.append(this.param);
			}
			if (this.hasInnerExpression()) {
				sb.append(" ");
				sb.append(this.get(0));
			}
			sb.append(">");
		}
	}

	final static Symbol symbolTableName(PNonTerminal n, Symbol table) {
		if (table == null) {
			String u = n.getLocalName().replace("~", "");
			return Symbol.unique(u);
		}
		return table;
	}

	public static class PSymbolAction extends PFunction<String> {
		public final Symbol table;

		public PSymbolAction(NezFunc op, String param, PNonTerminal e, Object ref) {
			super(op, param, e, ref);
			this.table = Symbol.nullUnique(param);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitSymbolAction(this, a);
		}

	}

	public static class PSymbolPredicate extends PFunction<String> {
		public Symbol table;
		public String symbol;

		public PSymbolPredicate(NezFunc op, String param, PNonTerminal pat, Object ref) {
			super(op, param, pat, ref);
			int l = param.indexOf('+');
			if (l > 0) {
				this.symbol = param.substring(l + 1);
				param = param.substring(0, l);
			}
			this.table = Symbol.unique(param);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitSymbolPredicate(this, a);
		}
	}

	public static class PSymbolScope extends PFunction<Symbol> {
		public PSymbolScope(NezFunc op, Symbol table, Expression e, Object ref) {
			super(op, table, e, ref);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitSymbolScope(this, a);
		}
	}

	public static class POnCondition extends PFunction<String> {
		public final boolean isPositive() {
			return !this.param.startsWith("!");
		}

		public final String flagName() {
			return this.param.startsWith("!") ? this.param.substring(1) : this.param;
		}

		public POnCondition(String c, Expression e, Object ref) {
			super(NezFunc.on, c, e, ref);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitOn(this, a);
		}
	}

	public static class PIfCondition extends PFunction<String> {
		public final boolean isPositive() {
			return !this.param.startsWith("!");
		}

		public final String flagName() {
			return this.param.startsWith("!") ? this.param.substring(1) : this.param;
		}

		public PIfCondition(String c, Object ref) {
			super(NezFunc._if, c, defaultEmpty, ref);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitIf(this, a);
		}
	}

	public static class PScan extends PFunction<String> {
		public final long mask;
		public final int shift;

		public PScan(String pattern, Expression e, Object ref) {
			super(NezFunc.scanf, pattern, e, ref);
			long bits = 0;
			int shift = 0;
			if (pattern != null) {
				bits = Long.parseUnsignedLong(pattern, 2);
				long m = bits;
				while ((m & 1L) == 0) {
					m >>= 1;
					shift++;
				}
				// factory.verbose("@@ mask=%s, shift=%d,%d", mask, bits,
				// shift);
			}
			this.mask = bits;
			this.shift = shift;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitScan(this, a);
		}
	}

	public static class PRepeat extends PFunction<Object> {
		public PRepeat(Expression e, Object ref) {
			super(NezFunc.repeat, null, e, ref);
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitRepeat(this, a);
		}
	}

	public static class PTrap extends PTerm {
		public int trapid;
		public int uid;

		public PTrap(int trapid, int uid, Object ref) {
			super(ref);
			this.trapid = trapid;
			this.uid = uid;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Expression.PTrap) {
				Expression.PTrap l = (Expression.PTrap) o;
				return this.trapid == l.trapid && this.uid == l.uid;
			}
			return false;
		}

		@Override
		public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
			return v.visitTrap(this, a);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("<trap " + this.trapid + " " + this.uid + ">");
		}
	}

	// utils

	public final static Expression newRange(int c, int c2, Object ref) {
		if (c == c2) {
			return new Expression.PByte(c, ref);
		} else {
			Expression.PByteSet b = new Expression.PByteSet(ref);
			b.set(c, c2, true);
			return b;
		}
	}

	public final static List<Expression> newList(int size) {
		return new ArrayList<>(size);
	}

	public final static void addSequence(List<Expression> l, Expression e) {
		if (e instanceof Expression.PEmpty) {
			return;
		}
		if (e instanceof Expression.PPair) {
			((Expression.PPair) e).expand(l);
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.get(l.size() - 1);
			if (prev instanceof Expression.PFail) {
				return;
			}
		}
		l.add(e);
	}

	public final static void addChoice(List<Expression> l, Expression e) {
		if (e instanceof Expression.PChoice) {
			for (int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if (e instanceof Expression.PFail) {
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.get(l.size() - 1);
			if (prev instanceof Expression.PEmpty) {
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

	public final static Expression newSequence(List<Expression> l, Object ref) {
		if (l.size() == 0) {
			return defaultEmpty;
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		return newSequence(l, 0, l.size(), ref);
	}

	private final static Expression newSequence(List<Expression> l, int start, int end, Object ref) {
		Expression first = l.get(start);
		if (start + 1 == end) {
			return first;
		}
		return newSequence(first, newSequence(l, start + 1, end, ref), ref);
	}

	// private final static Expression newPair(Expression e0, Expression e1,
	// Object ref) {
	// if (e0 instanceof Expression.Fail) {
	// return e0;
	// }
	// if (e1 instanceof Expression.Fail) {
	// return e1;
	// }
	// if (e1 instanceof Expression.Empty) {
	// return e0;
	// }
	// if (e0 instanceof Expression.Empty) {
	// return e1;
	// }
	// if (e0 instanceof Expression.Pair) {
	// List<Expression> l = Expression.newList(8);
	// ((Expression.Pair) e0).expand(l);
	// Expression.addSequence(l, e1);
	// return Expression.newSequence(l, ref);
	// }
	// return new Expression.Pair(e0, e1, ref);
	// }
	//
	public final static Expression newChoice(List<Expression> l, Object ref) {
		int size = l.size();
		boolean allCharacters = true;
		for (int i = 0; i < size; i++) {
			Expression e = l.get(i);
			if (e instanceof Expression.PEmpty) {
				size = i + 1;
				allCharacters = false;
				break;
			}
			if (e instanceof Expression.PByte || e instanceof Expression.PByteSet || e instanceof Expression.PAny) {
				continue;
			}
			allCharacters = false;
		}
		if (size == 1) {
			return l.get(0);
		}
		if (allCharacters) {
			Expression.PByteSet b = new Expression.PByteSet(ref);
			for (int i = 0; i < size; i++) {
				Expression e = l.get(i);
				if (e instanceof Expression.PAny) {
					return e;
				}
				if (e instanceof Expression.PByteSet) {
					b.union((Expression.PByteSet) e);
				} else {
					b.set(((Expression.PByte) e).byteChar, true);
				}
			}
			return b;
		}
		Expression[] inners = new Expression[size];
		for (int i = 0; i < size; i++) {
			inners[i] = l.get(i);
		}
		return new Expression.PChoice(inners, ref);
	}

	public final static Expression newSequence(Expression first, Expression second, Object ref) {
		if (first instanceof Expression.PFail) {
			return first;
		}
		if (second instanceof Expression.PFail) {
			return second;
		}
		if (second instanceof Expression.PEmpty) {
			return first;
		}
		if (first instanceof Expression.PEmpty) {
			return second;
		}
		return new Expression.PPair(first, second, ref).desugar();
	}

	public final static Expression append(Expression e1, Expression e2) {
		if (e1 instanceof Expression.PPair) {
			List<Expression> l = Expression.newList(64);
			((Expression.PPair) e1).expand(l);
			addSequence(l, e2);
			return newSequence(l, null);
		}
		return newSequence(e1, e2, null);
	}

	public final static Expression newString(byte[] utf8, Object ref) {
		if (utf8.length == 0) {
			return defaultEmpty;
		}
		if (utf8.length == 1) {
			return new Expression.PByte(utf8[0] & 0xff, ref);
		}
		List<Expression> l = newList(utf8.length);
		for (int i = 0; i < utf8.length; i++) {
			l.add(new Expression.PByte(utf8[i], ref));
		}
		return newSequence(l, ref);
	}

	public final static Expression newString(String text, Object ref) {
		return newString(OStringUtils.utf8(text), ref);
	}

	public final static Expression newTree(Expression e, Object ref) {
		return new Expression.PTree(false, null, 0, e, null, null, 0, ref);
	}

	public final static Expression newFoldTree(Symbol label, Expression e, Object ref) {
		return new Expression.PTree(true, label, 0, e, null, null, 0, ref);
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
				return e.getExternalReference();
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
				return new Expression.PNonTerminal(this.base, e.ns, lname, this.ref(e));
			}
			if (this.enableFullDuplication) {
				return new Expression.PNonTerminal(e.getGrammar(), e.getLocalName(), this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitEmpty(Expression.PEmpty e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PEmpty();
			}
			return e;
		}

		@Override
		public Expression visitFail(Expression.PFail e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PFail();
			}
			return e;
		}

		@Override
		public Expression visitByte(Expression.PByte e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PByte(e.byteChar, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitByteSet(Expression.PByteSet e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PByteSet(e.bits, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitAny(Expression.PAny e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PAny();
			}
			return e;
		}

		@Override
		public Expression visitPair(Expression.PPair e, A a) {
			Expression e0 = this.dup(e, 0, a);
			Expression e1 = this.dup(e, 1, a);
			if (e0 != e.get(0) || e1 != e.get(1) || this.enableFullDuplication) {
				return Expression.newSequence(e0, e1, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitChoice(Expression.PChoice e, A a) {
			boolean isModified = false;
			Expression[] en = new Expression[e.size()];
			for (int i = 0; i < e.size(); i++) {
				en[i] = this.dup(e, i, a);
				if (en[i] != e.get(i)) {
					isModified = true;
				}
			}
			if (isModified || this.enableFullDuplication) {
				return new Expression.PChoice(en, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitDispatch(Expression.PDispatch e, A a) {
			boolean isModified = false;
			Expression[] en = new Expression[e.size()];
			for (int i = 0; i < e.size(); i++) {
				en[i] = this.dup(e, i, a);
				if (en[i] != e.get(i)) {
					isModified = true;
				}
			}
			if (isModified || this.enableFullDuplication) {
				return new Expression.PDispatch(en, e.indexMap, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitOption(Expression.POption e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.POption(e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitRepetition(Expression.PRepetition e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PRepetition(e0, e.min, e.max, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitAnd(Expression.PAnd e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PAnd(e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitNot(Expression.PNot e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PNot(e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitTree(Expression.PTree e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PTree(e.folding, e.label, e.beginShift, e0, e.tag, e.value, e.endShift,
						this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitLinkTree(Expression.PLinkTree e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PLinkTree(e.label, e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitTag(Expression.PTag e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PTag(e.tag, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitReplace(PReplace e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PReplace(e.value, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitDetree(PDetree e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PDetree(e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitSymbolScope(Expression.PSymbolScope e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PSymbolScope(e.funcName, e.param, e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitSymbolAction(Expression.PSymbolAction e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PSymbolAction(e.funcName, e.param, (PNonTerminal) e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitSymbolPredicate(Expression.PSymbolPredicate e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PSymbolPredicate(e.funcName, e.param, (PNonTerminal) e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitScan(Expression.PScan e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PScan(e.param, e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitRepeat(Expression.PRepeat e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.PRepeat(e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitIf(Expression.PIfCondition e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PIfCondition(e.param, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitOn(Expression.POnCondition e, A a) {
			Expression e0 = this.dup(e, 0, a);
			if (e0 != e.get(0) || this.enableFullDuplication) {
				return new Expression.POnCondition(e.param, e0, this.ref(e));
			}
			return e;
		}

		@Override
		public Expression visitTrap(Expression.PTrap e, A a) {
			if (this.enableFullDuplication) {
				return new Expression.PTrap(e.trapid, e.uid, this.ref(e));
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
		public Expression visitEmpty(Expression.PEmpty e, A a) {
			return e;
		}

		@Override
		public Expression visitFail(Expression.PFail e, A a) {
			return e;
		}

		@Override
		public Expression visitByte(PByte e, A a) {
			return e;
		}

		@Override
		public Expression visitByteSet(Expression.PByteSet e, A a) {
			return e;
		}

		@Override
		public Expression visitAny(Expression.PAny e, A a) {
			return e;
		}

		@Override
		public Expression visitPair(Expression.PPair e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			e.set(1, this.rewrite(e, 1, a));
			return e.desugar();
		}

		@Override
		public Expression visitChoice(Expression.PChoice e, A a) {
			for (int i = 0; i < e.size(); i++) {
				e.set(i, this.rewrite(e, i, a));
			}
			return e;
		}

		@Override
		public Expression visitOption(Expression.POption e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitRepetition(Expression.PRepetition e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitAnd(Expression.PAnd e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitNot(Expression.PNot e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitDetree(Expression.PDetree e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitTree(Expression.PTree e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitLinkTree(Expression.PLinkTree e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitTag(Expression.PTag e, A a) {
			return e;
		}

		@Override
		public Expression visitReplace(Expression.PReplace e, A a) {
			return e;
		}

		@Override
		public Expression visitSymbolScope(Expression.PSymbolScope e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitSymbolAction(Expression.PSymbolAction e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitSymbolPredicate(Expression.PSymbolPredicate e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitScan(Expression.PScan e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitRepeat(Expression.PRepeat e, A a) {
			e.set(0, this.rewrite(e, 0, a));
			return e;
		}

		@Override
		public Expression visitIf(Expression.PIfCondition e, A a) {
			return e;
		}

		@Override
		public Expression visitOn(Expression.POnCondition e, A a) {
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
