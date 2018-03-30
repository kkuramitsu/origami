package origami.nez2;

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

	// public static <X> void testExpr(String expr, Function<Expr, X> f, X result) {
	// PEG peg = new PEG();
	// X r = f.apply(p(peg, new String[0], expr));
	// if (result == null) {
	// System.out.printf("%s <- %s\n", expr, r);
	// } else if (!r.equals(result)) {
	// System.err.printf("%s <- %s != %s\n", expr, r, result);
	// }
	// }
	//
	// //
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
