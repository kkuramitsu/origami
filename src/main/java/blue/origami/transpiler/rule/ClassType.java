package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TTypeCode;

public class ClassType implements TTypeRule {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TType type = this.parseType(env, t);
		if (type == null) {
			throw new TErrorCode(t, TFmt.undefined_type__YY0, t.getString());
		}
		return new TTypeCode(type);
	}

	public TType parseType(TEnv env, Tree<?> t) {
		String name = t.getString();
		TType ty = env.getType(name);
		if (ty == null) {
			switch (name) {
			case "bool":
			case "boolean":
				return TType.tBool;
			case "int":
			case "int32":
				return TType.tInt;
			case "double":
			case "float":
				return TType.tFloat;
			case "string":
				return TType.tString;
			}
		}
		return ty;
	}
}
