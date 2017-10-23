package blue.origami.transpiler.type;

import java.util.ArrayList;
import java.util.Arrays;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.OStrings;
import blue.origami.transpiler.code.Code;

public class UnionTy extends Ty {

	boolean isFinalized = false;
	Ty[] choice;

	public UnionTy(Ty... choice) {
		this.choice = choice;
		assert (choice.length > 0);
	}

	@Override
	public boolean isNonMemo() {
		return false;
	}

	@Override
	public Code getDefaultValue() {
		return this.choice[0].getDefaultValue();
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.joins(sb, this.choice, "|");
	}

	@Override
	public boolean hasVar() {
		return OArrays.testSomeTrue(t -> t.hasVar(), this.choice);
	}

	@Override
	public Ty finalTy() {
		if (!this.isFinalized) {
			if (this.choice.length == 1) {
				return this.choice[0].finalTy();
			}
			this.isFinalized = true;
			Ty[] choice = Arrays.stream(this.choice).map(t -> t.finalTy()).toArray(Ty[]::new);
			ArrayList<Ty> l = new ArrayList<>();
			for (Ty c : choice) {
				if (!this.contains(l, c)) {
					l.add(c);
				}
			}
			if (l.size() == 1) {
				return l.get(0);
			}
			this.choice = l.toArray(new Ty[l.size()]);
		}
		return this;
	}

	private boolean contains(ArrayList<Ty> l, Ty c) {
		for (Ty ty : l) {
			if (ty == c) {
				return true;
			}
			if (c.isMutable() && !ty.isMutable()) {
				// ODebug.trace("Union.contains %s %s %s %s", ty, c,
				// c.toImmutable(), ty == c.toImmutable());
				if (ty.equals(c.toImmutable())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		ArrayList<Ty> l = new ArrayList<>(this.choice.length);
		this.append(l, this);
		return new UnionTy(l.stream().map(t -> t.dupVar(dom)).toArray(Ty[]::new));
	}

	private void append(ArrayList<Ty> l, Ty ty) {
		if (ty instanceof UnionTy) {
			UnionTy uty = (UnionTy) ty;
			for (Ty t : uty.choice) {
				this.append(l, t.real());
			}
		} else {
			for (Ty t : l) {
				if (ty == t) {
					return;
				}
			}
			l.add(ty);
		}
	}

	// f(b)
	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		// ODebug.trace("UNION %s => %s", codeTy, this);
		if (codeTy.isUnion()) {
			UnionTy u = (UnionTy) codeTy.real();
			if (u == this) {
				return true;
			}
			ArrayList<Ty> l = new ArrayList<>();
			for (Ty t : u.choice) {
				t = this.contains(sub, t, logs);
				if (t == null) {
					return false;
				}
				l.add(t);
			}
			return logs.updateUnion(this, l.toArray(new Ty[l.size()]));
		}
		if (codeTy.isVar()) {
			return (codeTy.acceptTy(false, this, logs));
		}
		Ty matched = this.contains(sub, codeTy, logs);
		if (matched != null) {
			logs.updateUnion(this, matched);
			// ODebug.trace("UNION matched %s", codeTy);
			return true;
		}
		return false;
	}

	private Ty contains(boolean sub, Ty codeTy, VarLogger logs) {
		for (Ty ty : this.choice) {
			ODebug.trace("UNION[] %s => %s", codeTy, ty);
			if (ty.acceptTy(sub, codeTy, logs)) {
				return ty;
			}
		}
		return null;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return null;
	}
}
