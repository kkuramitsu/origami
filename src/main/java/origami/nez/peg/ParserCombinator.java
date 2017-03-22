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

package origami.nez.peg;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import origami.ODebug;
import origami.main.OOption;
import origami.nez.ast.SourcePosition;
import origami.nez.ast.Symbol;

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
				addMethodProduction(start, startMethod);
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
				addMethodProduction(name, m);
			}
		}
		return g;
	}

	private void addMethodProduction(String name, Method m) {
		try {
			Expression e = (Expression) m.invoke(this);
			grammar.addProduction(name, e);
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
				return Expression.newString(t.substring(1, t.length() - 1), src());
			}
			if (t.startsWith("@")) {
				return new Expression.PNonTerminal(grammar, t.substring(1), src());
			}
			if (t.startsWith("#")) {
				return new Expression.PTag(Symbol.unique(t.substring(1)), src());
			}
			if (t.startsWith("`") && t.endsWith("`")) {
				return new Expression.PReplace(t.substring(1, t.length() - 1), src());
			}
			return Expression.newString(t, src());
		}
		if (expr instanceof Character) {
			return Expression.newString(expr.toString(), src());
		}
		if (expr instanceof Expression) {
			return (Expression) expr;
		}
		return (Expression) expr;
	}

	protected final List<Expression> list(Object... exprs) {
		List<Expression> l = Expression.newList(exprs.length);
		for (Object expr : exprs) {
			Expression.addSequence(l, e(expr));
		}
		return l;
	}

	protected final Expression Expr(Object... exprs) {
		if (exprs.length == 0) {
			return Expression.defaultEmpty;
		}
		if (exprs.length == 1) {
			return e(exprs[0]);
		}
		return Expression.newSequence(list(exprs), src());
	}

	// protected final Expression P(String name) {
	// return new NonTerminal(grammar, name, src());
	// }
	//
	protected final Expression S(String token) {
		return Expression.newString(token, src());
	}

	protected final Expression t(char c) {
		return Expression.newString(String.valueOf(c), src());
	}

	protected final Expression Range(Character... chars) {
		Expression.PByteSet b = new Expression.PByteSet(src());
		for (int i = 0; i < chars.length; i += 2) {
			char s = chars[i];
			char e = chars[i + 1];
			b.set(s, e, true);
		}
		return b;
	}

	protected final Expression Choice(Object... exprs) {
		List<Expression> l = Expression.newList(exprs.length);
		for (Object expr : exprs) {
			Expression.addChoice(l, e(expr));
		}
		return Expression.newChoice(l, src());
	}

	protected final Expression Option(Object... exprs) {
		return new Expression.POption(Expr(exprs), src());
	}

	protected final Expression ZeroMore(Object... exprs) {
		return new Expression.PRepetition(Expr(exprs), 0, src());
	}

	protected final Expression OneMore(Object... exprs) {
		return new Expression.PRepetition(Expr(exprs), 1, src());
	}

	protected final Expression And(Object... exprs) {
		return new Expression.PAnd(Expr(exprs), src());
	}

	protected final Expression Not(Object... exprs) {
		return new Expression.PNot(Expr(exprs), src());
	}

	protected final Expression AnyChar() {
		return new Expression.PAny(src());
	}

	protected final Expression NotAny(Object... exprs) {
		return Expr(new Expression.PNot(Expr(exprs), src()), AnyChar());
	}

	protected final Expression Tree(Object... exprs) {
		return Expression.newTree(Expr(exprs), src());
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
		return Expression.newFoldTree(toSymbol(symbol), Expr(exprs), src());
	}

	protected final Expression OptionalFold(String label, Object... exprs) {
		Expression fold = Expression.newFoldTree(toSymbol(label), Expr(exprs), src());
		return new Expression.POption(fold, src());
	}

	protected final Expression ZeroMoreFold(String label, Object... exprs) {
		Expression fold = Expression.newFoldTree(toSymbol(label), Expr(exprs), src());
		return new Expression.PRepetition(fold, 0, src());
	}

	protected final Expression OneMoreFold(String label, Object... exprs) {
		Expression fold = Expression.newFoldTree(toSymbol(label), Expr(exprs), src());
		return new Expression.PRepetition(fold, 1, src());
	}

	protected Expression Link(String label, Expression e) {
		return new Expression.PLinkTree(toSymbol(label), e, src());
	}

	protected Expression Link(String label, String nonTerminal) {
		assert (nonTerminal.startsWith("@"));
		return new Expression.PLinkTree(toSymbol(label), e(nonTerminal), src());
	}

}
