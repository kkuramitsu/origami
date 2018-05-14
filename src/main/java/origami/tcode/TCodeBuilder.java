package origami.tcode;

import java.util.Arrays;

public class TCodeBuilder implements TSyntax {
	/* Literal */

	public static TCode b(boolean n) {
		return new TCode(null, n ? pTrue : pFalse, n, null);
	}

	public static TCode i(int n) {
		return new TCode(null, pInt, n, null);
	}

	public static TCode i(long n) {
		return new TCode(null, pInt, n, null);
	}

	public static TCode d(double n) {
		return new TCode(null, pDouble, n, null);
	}

	public static TCode c(char c) {
		return new TCode(null, pChar, c, null);
	}

	public static TCode s(String x) {
		return new TCode(null, pString, x, null);
	}

	public static TCode t(Class<?> c) {
		return new TCode(null, pType, c, null);
	}

	public static TCode conv(Object o) {
		if (o instanceof TCode) {
			return (TCode) o;
		}
		if (o instanceof Number) {
			if (o instanceof Double || o instanceof Float) {
				return d(((Number) o).doubleValue());
			}
			if (o instanceof Long) {
				return i(((Number) o).longValue());
			}
			return i(((Number) o).intValue());
		}
		if (o instanceof String) {
			String s = (String) o;
			if (s.startsWith("'") && s.endsWith("'")) {
				return s(s.substring(1, s.length() - 1));
			}
			return var(s);
		}
		if (o instanceof Boolean) {
			return b((Boolean) o);
		}
		if (o instanceof Character) {
			return c((Character) o);
		}
		return null;
	}

	public static TCode[] conv(Object[] o) {
		return Arrays.stream(o).map(a -> conv(a)).toArray(TCode[]::new);
	}

	/* Name */

	public static TCode var(String name) {
		return new TCode(null, pName, name, null);
	}

	public static TCode funcRef(String name) {
		return new TCode(null, pFuncRef, name, null);
	}

	/* operator */

	// public static TCode infix(TCode e, String op, TCode e2) {
	// return new TCode(null, op, e, e2);
	// }

	public static TCode apply(String name, Object... e) {
		return new TCode(null, name, conv(e));
	}

	public static TCode infix(Object e, String op, Object e2) {
		return new TCode(null, op, conv(e), conv(e2));
	}

	public static TCode unary(String op, Object e) {
		return new TCode(null, op, conv(e));
	}

	public static TCode and(TCode e, TCode e2) {
		return infix(e, "&&", e2);
	}

	public static TCode or(TCode e, TCode e2) {
		return infix(e, "||", e2);
	}

	public static TCode not(TCode e) {
		return unary("!", e);
	}

	public static TCode getter(Object e, String name) {
		return new TCode(null, pGetter, conv(e), var(name));
	}

	public static TCode getindex(Object e, Object n) {
		return new TCode(null, pGetIndex, conv(e), conv(n));
	}

	public static TCode macro(String name, TCode a) {
		return new TCode(null, pMacro, var(name), a);
	}

	public static TCode ifExpr(TCode a, Object b, Object c) {
		return new TCode(null, pIfExpr, a, conv(b), conv(c));
	}

	/* stmt */

	public static TCode block(TCode... a) {
		return new TCode(null, pBlock, a);
	}

	public static TCode letIn(String name, Object e) {
		return new TCode(null, pLetIn, var(name), conv(e));
	}

	public static TCode ifStmt(TCode a, TCode b) {
		return new TCode(null, pIf, a, b);
	}

	public static TCode ifStmt(TCode a, TCode b, TCode c) {
		return new TCode(null, pIfElse, a, b, c);
	}

	public static TCode whileStmt(TCode a, TCode b, TCode c) {
		return new TCode(null, pWhile, a, b, c);
	}

	/* func */

	public static TCode funcDecl(TCode name, TCode params, TCode body) {
		return new TCode(null, pFuncDecl, name, params, body);
	}

	public static TCode funcDecl(String name, String a, TCode body) {
		return new TCode(null, pFuncDecl, var(name), params(a), body);
	}

	public static TCode params(String... names) {
		return new TCode(null, pParams, Arrays.stream(names).map(n -> var(n)).toArray(TCode[]::new));
	}

	public static TCode param(String name) {
		return new TCode(null, pParam, name, null);
	}

	// static FuncDef define(String name, String... params) {
	// return new FuncDef(name, params);
	// }

	public final static void main(String[] a) {
		TSyntaxMapper m = new TSyntaxMapper();
		m.importSyntaxFile("/origami/syntax/js.syntax");
		TSourceSection sec = new TSourceSection(m, 0);
		sec.emit(apply("f", 1, "a", true));
		System.out.println(sec);
		sec = new TSourceSection(m, 0);
		sec.emit(letIn("a", infix(1, "+", 2)));
		System.out.println(sec);
		sec = new TSourceSection(m, 0);
		sec.emit(block(letIn("a", 1), letIn("b", 1)));
		System.out.println(sec);

		sec = new TSourceSection(m, 0);
		sec.emit(ifStmt(infix("a", "%", "b"), block(letIn("a", 1), letIn("b", 1))));
		System.out.println(sec);

	}

}
