package nez2;

import nez2.PEG.Expr;
import nez2.PEG.PTag;

public class First {

	static boolean nonnull(Expr pe) {
		switch (pe.ptag) {
		case Char:
			return true;
		case And:
		case Not:
		case Empty:
		case Many:
		case Tag:
		case Val:
		case If:
		case Exists:
		case Eval:
			return false;
		case Tree:
		case Link:
		case Fold:
		case Untree:
		case On:
		case Off:
		case Scope:
		case Symbol:
		case Match:
		case Contains:
		case Equals:
		case OneMore:
			return nonnull(pe.get(0));
		case Seq:
			return nonnull(pe.get(0)) || nonnull(pe.get(1));
		case Alt:
		case Or:
			return nonnull(pe.get(0)) && nonnull(pe.get(1));
		case DFA:
			for (Expr e : ((DFA) pe).indexed) {
				if (!nonnull(e)) {
					return false;
				}
			}
			return true;
		case NonTerm:
			Boolean r = (Boolean) pe.lookup("n$");
			if (r == null) {
				pe.memo("n$", false);
				r = nonnull(pe.get(0));
				pe.memo("n$", r);
			}
			return r;
		default:
			System.err.println("TODO: nonull " + pe);
			break;
		}
		return false;
	}

	static int fixlen(Expr pe) {
		int len = 0, len2 = 0;
		switch (pe.ptag) {
		case Empty:
		case And:
		case Not:
		case Tag:
		case Val:
		case If:
		case Exists:
		case Eval:
			return 0;
		case Char:
			return 1;
		case Tree:
		case Link:
		case Fold:
		case Untree:
		case On:
		case Off:
		case Scope:
		case Symbol:
		case Match:
		case Contains:
		case Equals:
			return fixlen(pe.get(0));
		case NonTerm:
			Integer n = (Integer) pe.lookup("flen$");
			if (n == null) {
				pe.memo("flen$", -1);
				n = fixlen(pe.get(0));
				pe.memo("flen$", n);
			}
			return n;
		case Seq:
			len = fixlen(pe.get(0));
			if (len != -1) {
				len2 = fixlen(pe.get(1));
				return len2 != -1 ? len + len2 : -1;
			}
			return -1;
		case Or:
		case Alt:
			len = fixlen(pe.get(0));
			len2 = fixlen(pe.get(1));
			return len == len2 ? len : -1;
		case DFA: {
			len = fixlen(pe.get(0));
			if (len == -1) {
				return -1;
			}
			for (int i = 1; i < pe.size(); i++) {
				if (len != fixlen(pe.get(i))) {
					return -1;
				}
			}
			return len;
		}
		default:
			return -1;
		}
	}

	static BitChar first(Expr pe) {
		switch (pe.ptag) {
		case Char:
			return (BitChar) pe.param(0);
		case Empty:
		case Tag:
		case Val:
		case If:
		case Exists:
		case Eval:
			return BitChar.AnySet;
		case And:
			if (pe.get(0).isChar()) {
				return (BitChar) pe.get(0).param(0);
			}
			return BitChar.AnySet;
		case Not:
			if (pe.get(0).isChar()) {
				return ((BitChar) pe.get(0).param(0)).not();
			}
			return BitChar.AnySet;
		case Many:
			if (pe.get(0).isChar()) {
				return (BitChar) pe.get(0).param(0);
			}
			return BitChar.AnySet;
		case OneMore:
			if (!pe.get(0).isNullable()) {
				return first(pe.get(0));
			}
			return BitChar.AnySet;
		case Tree:
		case Link:
		case Fold:
		case Untree:
		case On:
		case Off:
		case Scope:
		case Symbol:
		case Match:
		case Contains:
		case Equals:
			return first(pe.get(0));
		case Seq:
			if (!pe.isNullable()) {
				return first(pe.get(0));
			}
			return first(pe.get(0)).and(first(pe.get(1)));
		case Alt:
		case Or:
			return first(pe.get(0)).union(first(pe.get(1)));
		case NonTerm:
			BitChar r = (BitChar) pe.lookup("F$");
			if (r == null) {
				pe.memo("F$", BitChar.AnySet);
				r = first(pe.get(0));
				pe.memo("F$", r);
			}
			return r;
		default:
			System.err.println("TODO(first, " + pe + ")");
			break;
		}
		return null;
	}

	/* Reach */

	@FunctionalInterface
	interface Bool {
		public boolean get();
	}

	public static final Bool True = () -> true;
	public static final Bool False = () -> false;

	static class Unknown implements Bool {
		int count = 0;
		Bool ref = null;

		@Override
		public boolean get() {
			if (this.count > 0) {
				return false;
			}
			this.count++;
			return this.ref.get();
		}

		public Bool resolved(Bool res) {
			this.ref = res;
			return res;
		}
	}

	@FunctionalInterface
	interface Reached {
		public Bool check(Expr pe);
	}

	static Bool reach(Expr pe, String key, Reached f) {
		if (pe != null) {
			if (pe.isNonTerm()) {
				Bool res = (Bool) pe.lookup(key);
				if (res == null) {
					Unknown u = pe.memo(key, new Unknown());
					res = u.resolved(reach(pe.get(0), key, f));
				}
				return res;
			}
			Bool r = f.check(pe);
			if (r == True) {
				return r;
			}
			assert (r == False);
			final Bool r1 = reach(pe.get(0), key, f);
			final Bool r2 = reach(pe.get(1), key, f);
			if (r1 == True) {
				return r1;
			}
			if (r2 == True) {
				return r2;
			}
			if (r1 == False) {
				return r2; /* Unknown or False */
			}
			if (r2 == False) {
				return r1; /* Unknown */
			}
			return () -> r1.get() || r2.get();
		}
		return False;
	}

	static boolean reachFlag(Expr pe, final String flag) {
		return reach(pe, "f$" + flag, (p) -> {
			if (p.ptag == PTag.If && flag.equals(p.param(0))) {
				return True;
			}
			return False;
		}).get();
	}

	static boolean reachState(Expr pe) {
		return reach(pe, "st$", (p) -> {
			switch (p.ptag) {
			case Exists:
			case Match:
			case Contains:
			case Equals:
				return True;
			default:
				return False;
			}
		}).get();
	}

}
