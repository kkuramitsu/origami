package blue.origami.transpiler.type;

import java.util.ArrayList;
import java.util.Arrays;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.code.Code;
import blue.origami.util.ODebug;
import blue.origami.util.OStrings;

public class UnionTy extends Ty {

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
		return TArrays.testSomeTrue(t -> t.hasVar(), this.choice);
	}

	@Override
	public Ty finalTy() {
		Ty[] choice = Arrays.stream(this.choice).map(t -> t.finalTy()).toArray(Ty[]::new);
		if (choice.length == 1) {
			return choice[0];
		}
		ArrayList<Ty> l = new ArrayList<>();
		for (Ty c : choice) {
			if (!this.contains(l, c)) {
				l.add(c);
			}
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		return new UnionTy(l.toArray(new Ty[l.size()]));
	}

	private boolean contains(ArrayList<Ty> l, Ty c) {
		for (Ty ty : l) {
			if (ty == c) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return new UnionTy(Arrays.stream(this.choice).map(t -> t.dupVar(dom)).toArray(Ty[]::new));
	}

	// f(b)
	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		ODebug.trace("UNION %s => %s", codeTy, this);
		if (codeTy.isUnion()) {
			UnionTy u = (UnionTy) codeTy.real();
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
			ODebug.trace("UNION matched %s", codeTy);
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
	public <C> C mapType(TypeMap<C> codeType) {
		return null;
	}
}
