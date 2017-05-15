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

package blue.origami.nez.peg;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.peg.expression.PAnd;
import blue.origami.nez.peg.expression.PAny;
import blue.origami.nez.peg.expression.PByteSet;
import blue.origami.nez.peg.expression.PLinkTree;
import blue.origami.nez.peg.expression.PMany;
import blue.origami.nez.peg.expression.PNonTerminal;
import blue.origami.nez.peg.expression.PNot;
import blue.origami.nez.peg.expression.POption;
import blue.origami.nez.peg.expression.PTag;
import blue.origami.nez.peg.expression.PValue;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;

public class ParserCombinator {

	public ParserCombinator() {
	}

	protected Grammar grammar;
	protected OOption options;

	public final Grammar load(Grammar g, String start, OOption options) {
		this.grammar = g;
		this.options = options;
		Class<?> c = this.getClass();
		Method startMethod = null;
		if (start != null) {
			try {
				startMethod = c.getMethod("p" + start);
				this.addMethodProduction(start, startMethod);
			} catch (NoSuchMethodException | SecurityException e2) {
				options.verbose(e2.toString());
			}
		}
		for (Method m : c.getDeclaredMethods()) {
			if (m == startMethod) {
				continue;
			}
			if (m.getReturnType() == Expression.class && m.getParameterTypes().length == 0) {
				String name = m.getName();
				if (name.startsWith("p")) {
					name = name.substring(1);
				}
				this.addMethodProduction(name, m);
			}
		}
		return g;
	}

	private void addMethodProduction(String name, Method m) {
		try {
			Expression e = (Expression) m.invoke(this);
			this.grammar.addProduction(name, e);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			ODebug.traceException(e1);
		}
	}

	private SourcePosition src() {
		// Exception e = new Exception();
		// StackTraceElement[] stacks = e.getStackTrace();
		// System.out.println("^0 " + stacks[0]);
		// System.out.println("^1 " + stacks[1]);
		// System.out.println("^2 " + stacks[2]);
		// class JavaSourcePosition implements SourcePosition {
		// StackTraceElement e;
		//
		// JavaSourcePosition(StackTraceElement e) {
		// this.e = e;
		// }
		//
		// @Override
		// public Source getSource() {
		// return null;
		// }
		//
		// @Override
		// public long getSourcePosition() {
		// return 0;
		// }
		// }
		// return new JavaSourcePosition(stacks[2]);
		return SourcePosition.UnknownPosition;
	}

	protected final Expression e(Object expr) {
		if (expr instanceof String) {
			String t = (String) expr;
			if (t.startsWith("'") && t.endsWith("'")) {
				return Expression.newString(t.substring(1, t.length() - 1)).setSourcePosition(this.src());
			}
			if (t.startsWith("@")) {
				return new PNonTerminal(this.grammar, t.substring(1)).setSourcePosition(this.src());
			}
			if (t.startsWith("#")) {
				return new PTag(Symbol.unique(t.substring(1))).setSourcePosition(this.src());
			}
			if (t.startsWith("`") && t.endsWith("`")) {
				return new PValue(t.substring(1, t.length() - 1)).setSourcePosition(this.src());
			}
			return Expression.newString(t).setSourcePosition(this.src());
		}
		if (expr instanceof Character) {
			return Expression.newString(expr.toString()).setSourcePosition(this.src());
		}
		if (expr instanceof Expression) {
			return (Expression) expr;
		}
		return (Expression) expr;
	}

	protected final List<Expression> list(Object... exprs) {
		List<Expression> l = Expression.newList(exprs.length);
		for (Object expr : exprs) {
			Expression.addSequence(l, this.e(expr));
		}
		return l;
	}

	protected final Expression Expr(Object... exprs) {
		if (exprs.length == 0) {
			return Expression.defaultEmpty;
		}
		if (exprs.length == 1) {
			return this.e(exprs[0]);
		}
		return Expression.newSequence(this.list(exprs)).setSourcePosition(this.src());
	}

	// protected final Expression P(String name) {
	// return new NonTerminal(grammar, name).setSourcePosition(src());
	// }
	//
	protected final Expression S(String token) {
		return Expression.newString(token).setSourcePosition(this.src());
	}

	protected final Expression t(char c) {
		return Expression.newString(String.valueOf(c)).setSourcePosition(this.src());
	}

	protected final Expression Range(Character... chars) {
		PByteSet b = new PByteSet();
		for (int i = 0; i < chars.length; i += 2) {
			char s = chars[i];
			char e = chars[i + 1];
			b.set(s, e, true);
		}
		return b.setSourcePosition(this.src());
	}

	protected final Expression Choice(Object... exprs) {
		List<Expression> l = Expression.newList(exprs.length);
		for (Object expr : exprs) {
			Expression.addChoice(l, this.e(expr));
		}
		return Expression.newChoice(l).setSourcePosition(this.src());
	}

	protected final Expression Option(Object... exprs) {
		return new POption(this.Expr(exprs)).setSourcePosition(this.src());
	}

	protected final Expression ZeroMore(Object... exprs) {
		return new PMany(this.Expr(exprs), 0).setSourcePosition(this.src());
	}

	protected final Expression OneMore(Object... exprs) {
		return new PMany(this.Expr(exprs), 1).setSourcePosition(this.src());
	}

	protected final Expression And(Object... exprs) {
		return new PAnd(this.Expr(exprs)).setSourcePosition(this.src());
	}

	protected final Expression Not(Object... exprs) {
		return new PNot(this.Expr(exprs)).setSourcePosition(this.src());
	}

	protected final Expression AnyChar() {
		return new PAny().setSourcePosition(this.src());
	}

	protected final Expression NotAny(Object... exprs) {
		return this.Expr(new PNot(this.Expr(exprs)).setSourcePosition(this.src()), this.AnyChar());
	}

	protected final Expression Tree(Object... exprs) {
		return Expression.newTree(this.Expr(exprs)).setSourcePosition(this.src());
	}

	private Symbol toSymbol(String label) {
		if (label == null) {
			return null;
		}
		if (label.startsWith("$")) {
			return Symbol.unique(label.substring(1));
		}
		return Symbol.unique(label);
	}

	protected final Expression Fold(String symbol, Object... exprs) {
		return Expression.newFoldTree(this.toSymbol(symbol), this.Expr(exprs)).setSourcePosition(this.src());
	}

	protected final Expression OptionalFold(String label, Object... exprs) {
		Expression fold = Expression.newFoldTree(this.toSymbol(label), this.Expr(exprs)).setSourcePosition(this.src());
		return new POption(fold).setSourcePosition(this.src());
	}

	protected final Expression ZeroMoreFold(String label, Object... exprs) {
		Expression fold = Expression.newFoldTree(this.toSymbol(label), this.Expr(exprs)).setSourcePosition(this.src());
		return new PMany(fold, 0).setSourcePosition(this.src());
	}

	protected final Expression OneMoreFold(String label, Object... exprs) {
		Expression fold = Expression.newFoldTree(this.toSymbol(label), this.Expr(exprs)).setSourcePosition(this.src());
		return new PMany(fold, 1).setSourcePosition(this.src());
	}

	protected Expression Link(String label, Expression e) {
		return new PLinkTree(this.toSymbol(label), e).setSourcePosition(this.src());
	}

	protected Expression Link(String label, String nonTerminal) {
		assert (nonTerminal.startsWith("@"));
		return new PLinkTree(this.toSymbol(label), this.e(nonTerminal)).setSourcePosition(this.src());
	}

}
