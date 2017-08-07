package blue.origami.transpiler;

import blue.origami.transpiler.code.TCastCode;

public class VarDomain {
	VarTy[] dom = new VarTy[4];

	Ty newVarType(String name) {
		for (int i = 0; i < this.dom.length; i++) {
			if (this.dom[i] == null) {
				this.dom[i] = Ty.tVar(name);
				return this.dom[i];
			}
			if (name.equals(this.dom[i].getName())) {
				return this.dom[i];
			}
		}
		assert (name == null) : "extend the size of dom";
		return null;
	}

	public int mapCost() {
		int mapCost = 0;
		for (int i = 0; i < this.dom.length; i++) {
			if (this.dom[i] == null) {
				break;
			}
			Ty t = this.dom[i].realTy();
			if (t.isUntyped()) {
				mapCost += TCastCode.STUPID;
			}
			if (t == Ty.tBool || t == Ty.tInt || t == Ty.tFloat) {
				mapCost += TCastCode.CAST;
			}
		}
		return mapCost;
	}
}
