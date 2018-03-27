package nez2;

import java.util.Arrays;
import java.util.function.Function;

import nez2.PEG.Expr;
import nez2.PEG.ExprP;
import nez2.PEG.PTag;

@FunctionalInterface
public interface ParseFunc {
	boolean apply(ParserContext px);
}

// list x = (!x .)* x
// alt f = list('a')
// alt x y = x / list y
// list x y = list(x(y), )

// class Var extends Expr {
// int index = 0;
//
// Var(String name, int index) {
// }
//
// static ParserFunc c(int index) {
// return (px) -> {
// return px.get(index).apply(px);
// };
// }
// }
//
//// alt x y = x / list y
//// alt 'a'
//
// class App extends Expr {
// static ParserFunc c(ParserFunc inner, ParserFunc... args) {
// return (px) -> {
// ParserFunc[] stacked = px.push(args);
// boolean b = inner.apply(px);
// px.pop(stacked);
// return b;
// };
// }
// }
//
// class VarApp extends Expr {
// static ParserFunc c(int index, ParserFunc... args) {
// return (px) -> {
// ParserFunc f = px.get(index);
// ParserFunc[] stacked = px.push(args);
// boolean b = f.apply(px);
// px.pop(stacked);
// return b;
// };
// }
// }

// curry

class Var extends ExprP {
	int index;

	Var(String name, int index) {
		this.ptag = PTag.Var;
		this.label = name;
		this.index = index;
	}

	static ParseFunc c(int index) {
		return (px) -> {
			return px.get(index).apply(px);
		};
	}

	@Override
	public Object param(int index) {
		return index == 0 ? this.label : this.index;
	}

	@Override
	public int psize() {
		return 2;
	}

}

// S <- P('') !.
// P x <- a P(a x) / b P(b x) / x

// alt x y = x / y
// alt('a')

class App extends Expr {
	Expr func;
	Expr[] args;

	App(Expr f, Expr[] a) {
		this.ptag = PTag.App;
		this.func = f;
		this.args = a;
	}

	@Override
	public Expr get(int index) {
		return index == 0 ? this.func : null;
	}

	@Override
	public Object param(int index) {
		return this.args;
	}

	@Override
	public int psize() {
		return 1;
	}

	Expr[] applyArgs(Function<Expr, Expr> f) {
		return Arrays.stream(this.args).map(e -> f.apply(e)).toArray(Expr[]::new);
	}

	static ParseFunc curry(ParseFunc f, ParseFunc[] a) {
		return (px) -> {
			ParseFunc[] stacked = px.push(a);
			return px.pop(stacked, f.apply(px));
		};
	}

	static ParseFunc c(ParseFunc f, ParseFunc[] a) {
		return (px) -> {
			return curry(f, a).apply(px);
		};
	}

	static Expr expand(Expr pe, final Expr[] args) {
		switch (pe.ptag) {
		case Var:
			int index = (Integer) pe.param(1);
			if (index < args.length) {
				return args[index];
			}
			return PEG.Empty_;
		case App:
			Expr f = expand(pe.get(0), args);
			Expr[] p = ((App) pe).applyArgs((e) -> expand(e, args));
			if (f.isNonTerm()) {
				uname(f.p(0), p);
				return expand(f.get(0), p);
			}
			return new App(f, p);
		default:
			return PEG.dup(pe, (e) -> expand(e, args));
		}
	}

	static String uname(String name, Expr[] es) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		for (Expr e : es) {
			sb.append(e);
			e.strOut(sb);
		}
		return sb.toString();
	}
}
