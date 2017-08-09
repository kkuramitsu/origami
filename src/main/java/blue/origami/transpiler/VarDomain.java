package blue.origami.transpiler;

import blue.origami.transpiler.code.CastCode;

public class VarDomain {
	VarTy[] dom;

	public VarDomain(int n) {
		this.dom = new VarTy[n];
	}

	public Ty newVarType(String name) {
		String n = NameHint.safeName(name);
		for (int i = 0; i < this.dom.length; i++) {
			if (this.dom[i] == null) {
				this.dom[i] = Ty.tVar(n);
				return this.dom[i];
			}
			if (n.equals(this.dom[i].getName())) {
				return this.dom[i];
			}
		}
		assert (n == null) : "extend the size of dom";
		return null;
	}

	public int mapCost() {
		int mapCost = 0;
		for (int i = 0; i < this.dom.length; i++) {
			if (this.dom[i] == null) {
				break;
			}
			Ty t = this.dom[i].nomTy();
			if (t.isUntyped()) {
				mapCost += CastCode.STUPID;
			}
			if (t == Ty.tBool || t == Ty.tInt || t == Ty.tFloat) {
				mapCost += CastCode.CAST;
			}
		}
		return mapCost;
	}

	public void check() {
		char c = 'a';
		for (int i = 0; i < this.dom.length; i++) {
			if (this.dom[i] == null) {
				break;
			}
			Ty ty = this.dom[i].nomTy();
			if (ty == this.dom[i]) {
				this.dom[i].rename(String.valueOf(c++));
			}
			if (ty instanceof DataTy) {
				((DataTy) ty).asRecord();
			}
		}
	}

}
