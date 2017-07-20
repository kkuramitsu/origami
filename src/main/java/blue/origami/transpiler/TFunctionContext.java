package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.rule.NameExpr.TNameRef;

public class TFunctionContext {

	int count = 0;

	public TVariable newVariable(String name, TType type) {
		return new TVariable(this.count++, name, type);
	}

	public static class TVariable implements TNameRef {
		int index;
		String name;
		TType type;

		TVariable(int index, String name, TType type) {
			this.index = index;
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return this.name + this.index;
		}

		@Override
		public boolean isNameRef(TEnv env) {
			return true;
		}

		@Override
		public TCode nameCode(TEnv env, String name) {
			return new TNameCode(this.getName(), this.type);
		}
	}
}
