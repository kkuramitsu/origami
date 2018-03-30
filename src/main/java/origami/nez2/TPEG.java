package origami.nez2;

public class TPEG extends PEG {

	/* Tree Construction */

	public static class Tree extends OptimizedTree {

		public Tree(Expr inner) {
			this.ptag = PTag.Tree;
			this.label = null;
			this.inner = inner;
			assert (inner != null);
		}

		@Override
		Expr dup(Expr... es) {
			return new Tree(es[0]).dup(this);
		}
	}

	public static class Fold extends OptimizedTree {
		public Fold(String label, Expr inner) {
			this.ptag = PTag.Fold;
			this.label = label;
			this.inner = inner;
		}

		@Override
		Expr dup(Expr... es) {
			return new Fold(this.label, es[0]).dup(this);
		}
	}

	public static class Link extends ExprP1 {
		public Link(String label, Expr inner) {
			this.ptag = PTag.Link;
			this.label = label == null || label.length() == 0 ? origami.nez2.ParseTree.EmptyTag : label;
			this.inner = inner;
		}
	}

	public static class Tag extends ExprP {
		public Tag(String label) {
			this.ptag = PTag.Tag;
			this.label = label;
		}
	}

	public static class Val extends Expr {
		byte[] val;

		public Val(byte[] val) {
			this.ptag = PTag.Val;
			this.val = val;
		}

		@Override
		public byte[] charMap() {
			return this.val;
		}
	}

	public static class Untree extends Expr1 {
		public Untree(Expr inner) {
			this.ptag = PTag.Untree;
			this.inner = inner;
		}
	}

	public static class OptimizedTree extends ExprP1 {
		public int spos = 0;
		public int epos = 0;
		public String tag = null;
		public byte[] val = null;

		Expr dup(OptimizedTree s) {
			this.spos = s.spos;
			this.epos = s.epos;
			this.tag = s.tag;
			this.val = s.val;
			return this;
		}

		Expr optimize() {
			Expr[] es = this.inner.flatten(PTag.Seq);
			for (int i = es.length - 1; i >= 0; i--) {
				if (es[i].ptag == PTag.Tag) {
					this.tag = es[i].label();
					es[i] = PEG.Empty_;
					break;
				}
				if (!TPEG.isUnit(es[i])) {
					break;
				}
			}
			for (int i = es.length - 1; i >= 0; i--) {
				if (es[i].ptag == PTag.Val) {
					this.val = es[i].charMap();
					es[i] = PEG.Empty_;
					break;
				}
				if (!TPEG.isUnit(es[i])) {
					break;
				}
			}
			int start = es.length;
			for (int i = 0; i < es.length; i++) {
				int len = First.fixlen(es[i]);
				if (len == -1 || !TPEG.isUnit(es[i])) {
					start = i;
					break;
				}
				this.spos -= len;
			}
			this.inner = PEG.seq(start, es.length, es);
			if (start > 0) {
				Expr head = PEG.seq(0, start, es);
				return head.andThen(this);
			}
			return this;
		}

		@Override
		public Expr andThen(Expr next) {
			int len = First.fixlen(next);
			if (len != -1) {
				this.epos = -len;
				this.inner = this.inner.andThen(next);
				return this;
			}
			return super.andThen(next);
		}

	}

	// libs
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
			Hack.TODO(pe);
			break;
		}
		return false;
	}

	public static boolean isUnit(Expr pe) {
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
			Hack.TODO(pe);
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
			Hack.TODO(pe);
			break;
		}
		return false;
	}

	static Expr checkAST(Expr pe) {
		if (isTree(pe)) {
			return enforceTree(pe);
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
		case Tree:
		case Link:
		case Fold:
		case Untree:
			return enforceUnit(pe.get(0));
		case Tag:
		case Val:
			return PEG.Empty_;
		default:
			return PEG.dup(pe, TPEG::enforceUnit);
		}
	}

	static Expr enforceTree(Expr pe) {
		switch (pe.ptag) {
		case Tree:
			return pe.dup(enforceMut(pe.get(0)));
		case NonTerm:
			if (isTree(pe)) {
				return pe;
			}
			return new TPEG.Tree(pe);
		case Fold:
			return new TPEG.Tree(PEG.Empty_).andThen(pe.dup(enforceMut(pe.get(0))));
		case Link:
			return enforceUnit(pe.get(0));
		case Seq:
			if (isTree(pe.get(0))) {
				return enforceTree(pe.get(0)).andThen(enforceFold(pe.get(1)));
			}
			return enforceUnit(pe.get(0)).andThen(enforceTree(pe.get(1)));
		case Or:
			return enforceTree(pe.get(0)).orElse(enforceTree(pe.get(1)));
		case Alt:
			return enforceTree(pe.get(0)).orAlso(enforceTree(pe.get(1)));
		default:
			return enforceUnit(pe);
		}
	}

	static Expr enforceFold(Expr pe) {
		switch (pe.ptag) {
		case Fold:
			return pe.dup(enforceMut(pe.get(0)));
		case Tree:
			return new TPEG.Fold("", enforceMut(pe.get(0)));
		case NonTerm:
		case Link:
		case Tag:
		case Val:
			return enforceUnit(pe);
		default:
			return PEG.dup(pe, TPEG::enforceFold);
		}
	}

	static Expr enforceMut(Expr pe) {
		switch (pe.ptag) {
		case Tree:
			return new Link("", enforceTree(pe.get(0)));
		case Link:
			return new Link(pe.label(), enforceTree(pe.get(0)));
		case Fold:
			return new Link(pe.label(), new TPEG.Tree(enforceMut(pe.get(0))));
		case Tag:
		case Val:
			return pe;
		case NonTerm:
			if (isTree(pe)) {
				return new Link("", pe);
			}
			return pe;
		default:
			return PEG.dup(pe, TPEG::enforceMut);
		}
	}

}
