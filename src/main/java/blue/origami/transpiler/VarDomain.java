package blue.origami.transpiler;

import blue.origami.transpiler.code.CastCode;

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
}
