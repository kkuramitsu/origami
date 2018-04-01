package origami.nez2;

import java.io.IOException;
import java.util.function.Function;

import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.Production;
import blue.origami.parser.peg.SourceGrammar;

public class Hack {
	static boolean isVerbose = true;

	public static void TODO(Object... msg) {
		if (isVerbose) {
			StackTraceElement[] s = Thread.currentThread().getStackTrace();
			System.err.print("[TODO:" + s[2].getClassName() + "." + s[2].getMethodName() + "]");
			for (Object a : msg) {
				System.err.print(" " + a);
			}
			System.err.println();
		}
	}

	// test interface

	static boolean AssertMode = true;

	public static PEG expr(String pe) {
		PEG peg = new PEG();
		Loader.quickDef(peg, "A = " + pe);
		return peg;
	}

	public static void testLoad(String path) throws IOException {
		PEG peg = new PEG();
		peg.load(path);
		PEG peg2 = new PEG();
		Loader gl = new Loader(peg2);
		gl.setBasePath(path);
		gl.load(peg.toString());
		peg.forEach(n -> {
			Expr e1 = peg.get(n);
			Expr e2 = peg2.get(n);
			if (!e1.eq(e2)) {
				System.err.println(n + "\n\t" + e1 + "\n\t" + e2);
			}
		});
	}

	public static void testLoad2(String path) throws IOException {
		PEG peg = new PEG();
		peg.load(path);
		Grammar g = SourceGrammar.loadFile(path);
		peg.forEach(n -> {
			Expr e1 = peg.get(n);
			Production p = g.getProduction(n);
			if (!e1.toString().equals(p.getExpression().toString())) {
				System.err.println(n + "\n\t" + e1 + "\n\t" + p.getExpression());
			}
		});
	}

	public static <X> void testFunc(String ac, String expr, Function<Expr, Object> f, Object... a) throws IOException {
		PEG peg = new PEG();
		peg.define("A = " + expr);
		Expr pe = peg.get("A");
		String r = f.apply(pe).toString();
		if (a.length == 0) {
			System.out.printf("[TODO] %s:: %s -> %s\n", ac, expr, r);
		} else if (r.equals(a[0].toString())) {
			System.out.printf("[succ] %s:: %s -> %s\n", ac, expr, r);
		} else {
			System.err.printf("[fail] %s:: %s -> %s != %s\n", ac, expr, r, a[0]);
			if (AssertMode) {
				assert r.equals(a[0].toString());
			}
		}
	}

	// public static void testMatch(String expr, String... args) throws Throwable {
	// PEG peg = new PEG();
	// if (expr.startsWith("/") || expr.endsWith(".opeg")) {
	// peg.load(expr);
	// } else {
	// Loader.quickDef(peg, expr);
	// }
	// Parser p = peg.getParser();
	// for (int i = 0; i < args.length; i += 2) {
	// String r = p.parse(args[i]).toString();
	// if (r.equals(args[i + 1])) {
	// System.out.printf("[succ] %s %s => %s\n", expr, args[i], r);
	// } else {
	// System.err.printf("[fail] %s %s => %s != %s\n", expr, args[i], r, args[i +
	// 1]);
	// }
	// }
	// }

}
