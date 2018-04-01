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

		// @Override
		// public Expr andThen(Expr next) {
		// int len = First.fixlen(next);
		// if (len != -1) {
		// System.err.println("fixlen = " + len + " " + next);
		// this.epos = -len;
		// this.inner = this.inner.andThen(next);
		// return this;
		// }
		// return super.andThen(next);
		// }

	}

	public static enum TreeState {
		Unit, Tree, Mut, Fold, Unknown;
	}

	public static TreeState ts(Expr pe) {
		switch (pe.ptag) {
		case Tree:
			return TreeState.Tree;
		case Fold:
			return TreeState.Fold;
		case Link:
		case Tag:
		case Val:
			return TreeState.Mut;
		case Untree:
		case Char:
		case Not:
		case Empty:
		case If:
		case Exists:
		case Match:
		case Eval:
			return TreeState.Unit;
		case And:
		case OneMore:
		case Many: {
			TreeState ts = ts(pe.get(0));
			if (ts == TreeState.Tree) {
				return TreeState.Mut;
			}
			return ts;
		}
		case On:
		case Off:
		case State:
		case Scope:
		case Symbol:
		case Contains:
		case Equals:
			return ts(pe.get(0));
		case Seq: {
			TreeState ts1 = ts(pe.get(0));
			if (ts1 == TreeState.Unit) {
				return ts(pe.get(1));
			}
			return ts1;
		}
		case Alt:
		case Or: {
			TreeState ts1 = ts(pe.get(0));
			if (ts1 == TreeState.Unit) {
				return ts(pe.get(1)) == TreeState.Mut ? TreeState.Mut : TreeState.Unit;
			}
			if (ts1 == TreeState.Tree) { /* Tree / '' */
				return ts(pe.get(1)) == TreeState.Unit ? TreeState.Mut : ts1;
			}
			return ts1;
		}
		case NonTerm:
			TreeState r = (TreeState) pe.lookup("ts$");
			if (r == null) {
				pe.memo("ts$", TreeState.Unknown);
				r = ts(pe.get(0));
				r = r == TreeState.Unknown ? TreeState.Unit : r;
				pe.memo("ts$", r);
				return r;
			}
			return r;
		default:
			Hack.TODO(pe);
			if (pe.size() == 1) {
				return ts(pe.get(0));
			}
			break;
		}
		return TreeState.Unit;
	}

	public static boolean isUnit(Expr pe) {
		return ts(pe) == TreeState.Unit;
	}

	static boolean isTree(Expr pe) {
		return ts(pe) == TreeState.Tree;
	}

	static boolean isMut(Expr pe) {
		return ts(pe) == TreeState.Mut;
	}

	static boolean isFold(Expr pe) {
		return ts(pe) == TreeState.Fold;
	}

	static Expr checkAST(Expr pe) {
		// d("checkAST", pe);
		if (isTree(pe)) {
			return enforceTree(pe);
		}
		if (isMut(pe)) {
			return enforceMut(pe);
		}
		if (isFold(pe)) {
			return enforceFold(pe);
		}
		return enforceUnit(pe);
	}

	static void d(String ac, Expr pe) {
		System.err.printf("%s {utm=%s %s %s %s} %s \n", ac, isUnit(pe), isTree(pe), isMut(pe), isFold(pe), pe);
	}

	static Expr enforceUnit(Expr pe) {
		switch (pe.ptag) {
		case NonTerm:
			if (isUnit(pe)) {
				return pe;
			}
			d("!enforceUnit", pe);
			return pe;
		case Tree:
		case Link:
		case Fold:
			d("removeTree", pe);
			return enforceUnit(pe.get(0));
		case Tag:
		case Val:
			return PEG.Empty_;
		case Not:
			return new Not(checkAST(pe.get(0)));
		default:
			return PEG.dup(pe, TPEG::enforceUnit);
		}
	}

	static Expr enforceTree(Expr pe) {
		switch (pe.ptag) {
		case Tree:
			return pe.dup(enforceMut(pe.get(0)));
		case NonTerm:
			if (isTree(pe) || isUnit(pe)) {
				return pe;
			}
			d("enforceTree", pe);
			return new TPEG.Tree(pe);
		case Fold:
			d("enforceTree", pe);
			return new TPEG.Tree(PEG.Empty_).andThen(pe.dup(enforceMut(pe.get(0))));
		case Link:
			d("enforceTree", pe);
			return enforceTree(pe.get(0));
		case Seq:
			if (isUnit(pe.get(0))) {
				return enforceUnit(pe.get(0)).andThen(enforceTree(pe.get(1)));
			}
			// System.out.println("enforceFold => " + pe.get(0) + " ++ " + pe.get(1));
			return enforceTree(pe.get(0)).andThen(enforceFold(pe.get(1)));
		case Alt:
		case Or:
			return enforceTree(pe.get(0)).orElse(enforceTree(pe.get(1)));
		default:
			Hack.TODO(pe);
			return PEG.dup(pe, TPEG::enforceUnit);
		}
	}

	static Expr enforceMut(Expr pe) {
		switch (pe.ptag) {
		case NonTerm:
			if (isUnit(pe) || isMut(pe)) {
				return pe;
			}
			if (isTree(pe)) {
				return new Link("", pe);
			}
			return pe;
		case Link:
			return new Link(pe.label(), enforceTree(pe.get(0)));
		case Tree:
			return new Link("", enforceTree(pe.get(0)));
		case Fold:
			return new Link(pe.label(), new TPEG.Tree(enforceMut(pe.get(0))));
		case Tag:
		case Val:
			return pe;
		default:
			if (isUnit(pe)) {
				return pe;
			}
			// System.out.println("@@ " + pe);
			return PEG.dup(pe, TPEG::enforceMut);
		}
	}

	static Expr enforceFold(Expr pe) {
		switch (pe.ptag) {
		case NonTerm:
			if (isUnit(pe) || isFold(pe)) {
				return pe;
			}
			if (isTree(pe)) {
				d("enforceFold", pe);
				return new TPEG.Fold("", new TPEG.Link("", pe));
			}
			d("!enforceFold", pe);
			return pe;
		case Fold:
			return pe.dup(enforceMut(pe.get(0)));
		case Tree:
			d("enforceFold", pe);
			return new TPEG.Fold("", enforceMut(pe.get(0)));
		case Link:
			d("enforceFold", pe);
			return new TPEG.Fold("", enforceMut(pe));
		case Tag:
		case Val:
			return PEG.Empty_;
		default:
			return PEG.dup(pe, TPEG::enforceFold);
		}
	}

}
