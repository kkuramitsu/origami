package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TParamCode extends TArgCode {

	public TParamCode(TTemplate template, TCode... args) {
		super(template.getReturnType(), template, args.clone());
	}

	public int checkParam(TEnv env) {
		int mapCost = 0;
		TType[] p = this.template.getParamTypes();
		for (int i = 0; i < this.args.length; i++) {
			// System.out.printf("FIXME %d %s %s\n", i, this.args[i].getType(),
			// p[i]);
			this.args[i] = this.args[i].asType(env, p[i]);
			if (this.args[i] instanceof TCastCode) {
				mapCost += ((TCastCode) this.args[i]).getMapCost();
				if (i < 2) {
					mapCost += ((TCastCode) this.args[i]).getMapCost();
				}
			}
		}
		return mapCost;
	}

}