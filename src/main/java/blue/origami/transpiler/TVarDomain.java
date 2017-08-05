package blue.origami.transpiler;

import blue.origami.transpiler.code.TCastCode;

public class TVarDomain {
	TVarType[] dom = new TVarType[4];

	TType newVarType(String name) {
		for (int i = 0; i < this.dom.length; i++) {
			if (this.dom[i] == null) {
				this.dom[i] = TType.tVar(name);
				return this.dom[i];
			}
			if (name.equals(this.dom[i].getName())) {
				return this.dom[i];
			}
		}
		assert (name == null) : "extend dom size";
		return null;
	}

	public int mapCost() {
		int mapCost = 0;
		for (int i = 0; i < this.dom.length; i++) {
			if (this.dom[i] == null) {
				break;
			}
			TType t = this.dom[i].realType();
			if (t.isUntyped()) {
				mapCost += TCastCode.STUPID;
			}
			if (t == TType.tBool || t == TType.tInt || t == TType.tFloat) {
				mapCost += TCastCode.CAST;
			}
		}
		return mapCost;
	}
}
