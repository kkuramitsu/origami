package nez2;

import nez2.PEG.Expr;
import nez2.PEG.Link;

public class Trees {

	static boolean isTreeMut(Expr pe) {
		switch (pe.ptag) {
		case Tree:
		case Fold:
		case Link:
		case Tag:
		case Val:
			return true;
		case Untree:
		case Char:
		case Not:
		case Empty:
		case If:
		case Exists:
		case Eval:
			return false;
		case And:
		case OneMore:
		case Many:
		case On:
		case Off:
		case Scope:
		case Symbol:
		case Match:
		case Contains:
		case Equals:
			return isTreeMut(pe.get(0));
		case Seq:
			return isTreeMut(pe.get(0)) || isTreeMut(pe.get(1));
		case Alt:
		case Or:
			return isTreeMut(pe.get(0)) || isTreeMut(pe.get(1));
		case DFA:
			for (int i = 0; i < pe.size(); i++) {
				if (isTreeMut(pe.get(0))) {
					return true;
				}
			}
			return false;
		case NonTerm:
			Boolean r = (Boolean) pe.lookup("tm$");
			if (r == null) {
				pe.memo("tm$", false);
				r = isTree(pe.get(0));
				pe.memo("tm$", r);
			}
			return r;
		default:
			System.err.println("TODO: isTreeMut " + pe);
			break;
		}
		return false;
	}

	static boolean isUnit(Expr pe) {
		return !isTreeMut(pe);
	}

	static boolean isUnit2(Expr pe) {
		switch (pe.ptag) {
		case Tree:
		case Fold:
		case Link:
		case Tag:
		case Val:
			return false;
		case Untree:
		case Char:
		case Not:
		case Empty:
		case If:
		case Exists:
		case Eval:
			return true;
		case And:
		case OneMore:
		case Many:
		case On:
		case Off:
		case Scope:
		case Symbol:
		case Match:
		case Contains:
		case Equals:
			return isUnit2(pe.get(0));
		case Seq:
			return isUnit2(pe.get(0)) && isUnit2(pe.get(1));
		case Alt:
		case Or:
			return isUnit2(pe.get(0)) && isUnit2(pe.get(1));
		case NonTerm:
			Boolean r = (Boolean) pe.lookup("u$");
			if (r == null) {
				pe.memo("u$", true);
				r = isTree(pe.get(0));
				pe.memo("u$", r);
			}
			return r;
		default:
			System.err.println("TODO: unit " + pe);
			break;
		}
		return true;
	}

	static boolean isTree(Expr pe) {
		switch (pe.ptag) {
		case Tree:
		case Fold:
			return true;
		case Link:
		case Untree:
		case Char:
		case Not:
		case Empty:
		case Tag:
		case Val:
		case If:
		case Exists:
		case Eval:
			return false;
		case And:
		case OneMore:
		case Many:
		case On:
		case Off:
		case Scope:
		case Symbol:
		case Match:
		case Contains:
		case Equals:
			return isTree(pe.get(0));
		case Seq:
			return isTree(pe.get(0)) || isTree(pe.get(1));
		case Alt:
		case Or:
			return isTree(pe.get(0)) || isTree(pe.get(1));
		case DFA:
			for (int i = 0; i < pe.size(); i++) {
				if (isTree(pe.get(0))) {
					return true;
				}
			}
			return false;
		case NonTerm:
			Boolean r = (Boolean) pe.lookup("t$");
			if (r == null) {
				pe.memo("t$", false);
				r = isTree(pe.get(0));
				pe.memo("t$", r);
			}
			return r;
		default:
			System.err.println("TODO: tree " + pe);
			break;
		}
		return false;
	}

	static Expr checkAST(Expr pe) {
		if (isTree(pe)) {
			return checkTree(pe);
		}
		if (!isUnit(pe)) {
			return checkMut(pe);
		}
		return enforceUnit(pe);
	}

	static Expr enforceUnit(Expr pe) {
		switch (pe.ptag) {
		case NonTerm:
			if (isUnit(pe)) {
				return pe;
			}
			System.err.printf("enforceUnit %s isUnit=%s isTree=%s\n", pe, isUnit(pe), isTree(pe));
			throw new RuntimeException();
			// if (isTree(pe)) {
			// return new PEG.Untree(pe);
			// }
			// return new PEG.Untree(new PEG.Tree(pe));
		case Tree:
		case Link:
		case Fold:
		case Untree:
			return enforceUnit(pe.get(0));
		case Tag:
		case Val:
			return PEG.Empty_;
		default:
			return PEG.dup(pe, Trees::enforceUnit);
		}
	}

	static Expr checkTree(Expr pe) {
		switch (pe.ptag) {
		case Tree:
			return pe.dup(null, checkMut(pe.get(0)));
		case NonTerm:
			if (isTree(pe)) {
				return pe;
			}
			return new PEG.Tree(pe);
		case Fold:
			return new PEG.Tree(PEG.Empty_).andThen(new PEG.Fold((String) pe.param(0), checkMut(pe.get(0))));
		case Link:
			return enforceUnit(pe.get(0));
		case Seq:
			if (isTree(pe.get(0))) {
				return checkTree(pe.get(0)).andThen(checkFold(pe.get(1)));
			}
			return enforceUnit(pe.get(0)).andThen(checkTree(pe.get(1)));
		case Or:
		case Alt:
			if (isTree(pe.get(0))) {
				return enforceTree(pe.get(0)).orElse(enforceTree(pe.get(1)));
			}
			return enforceUnit(pe.get(0)).orElse(enforceUnit(pe.get(1)));
		default:
			return enforceUnit(pe);
		}
	}

	static Expr enforceTree(Expr pe) {
		pe = checkTree(pe);
		if (!isTree(pe)) {
			return new PEG.Tree(pe);
		}
		return pe;
	}

	static Expr checkFold(Expr pe) {
		switch (pe.ptag) {
		case Fold:
			return pe.dup(pe.p(0), checkMut(pe.get(0)));
		case Tree:
			return new PEG.Fold("", checkMut(pe.get(0)));
		case NonTerm:
		case Link:
		case Tag:
		case Val:
			return enforceUnit(pe);
		default:
			return PEG.dup(pe, Trees::checkFold);
		}
	}

	static Expr checkMut(Expr pe) {
		switch (pe.ptag) {
		case Tree:
			return new Link("", enforceTree(pe.get(0)));
		case Link:
			return new Link((String) pe.param(0), enforceTree(pe.get(0)));
		case Fold:
			return new Link((String) pe.param(0), new PEG.Tree(checkMut(pe.get(0))));
		case Tag:
		case Val:
			return pe;
		case NonTerm:
			if (isTree(pe)) {
				return new Link("", pe);
			}
			return pe;
		default:
			return PEG.dup(pe, Trees::checkMut);
		}
	}

}
