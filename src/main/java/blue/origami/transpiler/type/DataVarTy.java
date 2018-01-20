package blue.origami.transpiler.type;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;

public class DataVarTy extends DataTy {

	public DataVarTy() {
		super();
	}

	@Override
	public Ty memoed() {
		return Ty.tData(this.fields());
	}

	@Override
	public Ty resolveFieldType(Env env, AST s, String name) {
		if (!this.hasField2(name)) {
			this.fields.add(name);
		}
		return super.resolveFieldType(env, s, name);
	}
}