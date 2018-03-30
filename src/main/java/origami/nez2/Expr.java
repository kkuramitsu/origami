package origami.nez2;

import java.util.ArrayList;

import origami.nez2.PEG.Alt;
import origami.nez2.PEG.Char;
import origami.nez2.PEG.Empty;
import origami.nez2.PEG.Not;
import origami.nez2.PEG.Or;
import origami.nez2.PEG.Seq;

public class Expr implements OStrings {
	public static enum PTag {
		Empty, Char, Seq, Or, Alt, And, Not, Many, OneMore, NonTerm, // pattern
		Tree, Link, Fold, Tag, Val, Untree, // tree construction
		State, Scope, Symbol, Match, Exists, Equals, Contains, // state
		If, On, Off, // conditional parsing
		Var, App, Param, // Higher ordered
		DFA, // DFA
		Bugs, Eval, Unary;
	}

	PTag ptag;
	Object value;

	public PTag tag() {
		return this.ptag;
	}

	public boolean eq(Expr pe) {
		return this.ptag == pe.ptag && this.toString().equals(pe.toString());
	}

	Expr dup(Expr... inners) {
		return this;
	}

	public int size() {
		return 0;
	}

	public Expr get(int index) {
		return null;
	}

	public String label() {
		return null;
	}

	public BitChar bitChar() {
		return null;
	}

	public byte[] charMap() {
		return null;
	}

	public int index() {
		return -1;
	}

	public String[] params() {
		return null;
	}

	// public int psize() {
	// return 0;
	// }
	//
	// public Object param(int index) {
	// return null;
	// }

	public Expr[] flatten(PTag target) {
		return new Expr[] { this };
	}

	public final Object get() {
		return this.value;
	}

	public final Expr set(Object value) {
		this.value = value;
		return this;
	}

	public Object lookup(String key) {
		return null;
	}

	public <V> V memo(String key, V u) {
		return u;
	}

	@Override
	public void strOut(StringBuilder sb) {
		PEG.showing(sb, this);
	}

	@Override
	public final String toString() {
		return OStrings.stringfy(this);
	}

	public boolean isEmpty() {
		return this instanceof Empty;
	}

	public boolean isFail() {
		return this instanceof Not && this.get(0).isEmpty();
	}

	public boolean isChar() {
		return this instanceof Char;
	}

	public boolean isStr() {
		return false;
	}

	public boolean isNonTerm() {
		return this.ptag == PTag.NonTerm;
	}

	public boolean isAny() {
		return false;
	}

	public boolean isOption() {
		return false;
	}

	public Expr andThen(Expr pe) {
		if (pe == null || pe.isEmpty()) {
			return this;
		}
		return new Seq(this, pe);
	}

	public Expr orElse(Expr pe) {
		if (this.isEmpty() || this.isOption()) {
			return this;
		}
		return new Or(this, pe);
	}

	public Expr orAlso(Expr pe) {
		return new Alt(this, pe);
	}

	public boolean isNullable() {
		return !First.nonnull(this);
	}

	public Expr deref() {
		return this;
	}

	public boolean isApp() {
		return false;
	}

}

class ExprP extends Expr {
	String label;

	@Override
	public String label() {
		return this.label;
	}

}

class Expr1 extends Expr {
	Expr inner;

	@Override
	public int size() {
		return 1;
	}

	@Override
	public Expr get(int index) {
		return (index == 0) ? this.inner : null;
	}

}

class ExprP1 extends Expr1 {
	String label;

	@Override
	public String label() {
		return this.label;
	}
}

class Expr2 extends Expr {
	Expr left;
	Expr right;

	@Override
	public int size() {
		return 2;
	}

	@Override
	public Expr get(int index) {
		switch (index) {
		case 0:
			return this.left;
		case 1:
			return this.right;
		default:
			return null;
		}
	}

	@Override
	public Expr[] flatten(PTag target) {
		if (this.ptag == target) {
			ArrayList<Expr> l = new ArrayList<>();
			this.listup(l);
			return l.toArray(new Expr[l.size()]);
		}
		return super.flatten(target);
	}

	private void listup(ArrayList<Expr> l) {
		if (this.left instanceof Expr2 && this.left.ptag == this.ptag) {
			((Expr2) this.left).listup(l);
		} else {
			l.add(this.left);
		}
		if (this.right instanceof Expr2 && this.right.ptag == this.ptag) {
			((Expr2) this.right).listup(l);
		} else {
			l.add(this.right);
		}
	}
}
