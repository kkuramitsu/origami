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
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PTree;
import blue.origami.util.OStringUtils;
import blue.origami.util.StringCombinator;

public abstract class Expression extends AbstractList<Expression> implements StringCombinator {

	/* External reference */

	private Object ref = null;

	public final Object getRef() {
		return this.ref;
	}

	public final Expression setRef(Object ref) {
		this.ref = ref;
		return this;
	}

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

	public abstract <V, A> V visit(ExpressionVisitor<V, A> v, A a);

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

	public Expression desugar() {
		return this;
	}

	/* Equals */

	@Override
	public final boolean equals(Object o) {
		if (o.getClass() == this.getClass() && this.equalsInner((Expression) o)) {
			Object[] p1 = this.extract();
			Object[] p2 = ((Expression) o).extract();
			if (p1.length == p2.length) {
				for (int i = 0; i < p1.length; i++) {
					if (!p1[i].equals(p2[i])) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	private static Object[] emptyObjects = new Object[0];

	protected Object[] extract() {
		return emptyObjects;
	}

	private boolean equalsInner(Expression e) {
		if (this.size() == e.size()) {
			for (int i = 0; i < this.size(); i++) {
				if (!this.get(i).equals(e.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/* Unary */

	public final static boolean isMultiBytes(Expression e) {
		if (e instanceof PPair) {
			Expression left = ((PPair) e).left;
			if (left instanceof PByte) {
				return isMultiBytes(((PPair) e).right);
			}
			return false;
		}
		if (e instanceof PByte) {
			return true;
		}
		return false;
	}

	public final static Expression extractMultiBytes(Expression e, List<java.lang.Integer> l) {
		if (e instanceof PPair) {
			Expression left = ((PPair) e).left;
			if (left instanceof PByte) {
				if (l != null) {
					l.add(((PByte) left).byteChar());
				}
				return extractMultiBytes(((PPair) e).right, l);
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

	public final static byte[] toMultiBytes(List<java.lang.Integer> l) {
		byte[] b = new byte[l.size()];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) (int) l.get(i);
		}
		return b;
	}

	/* AST */

	// public final static void formatReplace(String value, StringBuilder sb) {
	// // value;
	// sb.append('`');
	// for (int i = 0; i < value.length(); i++) {
	// int c = value.charAt(i) & 0xff;
	// ByteSet.formatByte(c, "`", "\\x02x", sb);
	// }
	// sb.append('`');
	// }

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
		if (size == 1) {
			return l.get(0);
		}
		if (size == 2 && l.get(1) instanceof PEmpty) {
			return new POption(l.get(0));
		}
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
		return new PChoice(false, inners);
	}

	public final static Expression newUChoice(List<Expression> l) {
		int size = l.size();
		if (size == 1) {
			return l.get(0);
		}
		Expression[] inners = new Expression[size];
		for (int i = 0; i < size; i++) {
			inners[i] = l.get(i);
		}
		return new PChoice(true, inners);
	}

	/* Older version */
	public final static Expression newSequence0(Expression first, Expression second) {
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

	public final static Expression newSequence(Expression first, Expression second) {
		if (first instanceof PPair) {
			return newSequence(first.get(0), newSequence(first.get(1), second));
		}
		if (second instanceof PEmpty) {
			return first;
		}
		if (second instanceof PFail) {
			return second;
		}
		if (first instanceof PFail) {
			return first;
		}
		if (first instanceof PEmpty) {
			return second;
		}
		return new PPair(first, second);
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
			PNonTerminal next = (PNonTerminal) e;
			e = next.getProduction().getExpression();
		}
		return e;
	}

	// Visitor

}
