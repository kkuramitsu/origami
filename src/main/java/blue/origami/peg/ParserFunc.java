package blue.origami.peg;

import java.util.Arrays;
import java.util.function.Function;

import blue.origami.peg.PEG.CTag;
import blue.origami.peg.PEG.Expr;
import blue.origami.peg.PEG.ExprP;

@FunctionalInterface
public interface ParserFunc {
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
		this.ctag = CTag.Var;
		this.label = name;
		this.index = index;
	}

	static ParserFunc c() {
		return (px) -> {
			return px.get().apply(px);
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
		this.ctag = CTag.App;
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

	static ParserFunc curry(ParserFunc f, ParserFunc a) {
		return (px) -> {
			ParserFunc stacked = px.push(a);
			return px.pop(stacked, f.apply(px));
		};
	}

	static ParserFunc c(ParserFunc f, ParserFunc a) {
		return (px) -> {
			return curry(f, a).apply(px);
		};
	}

	static Expr expand(Expr pe, final Expr[] args) {
		switch (pe.ctag) {
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
				return expand(f.get(0), p);
			}
			return new App(f, p);
		default:
			return PEG.dup(pe, (e) -> expand(e, args));
		}
	}
}
