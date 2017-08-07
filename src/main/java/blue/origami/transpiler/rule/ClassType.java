package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TTypeCode;

public class ClassType implements ParseRule {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		Ty type = this.parseType(env, t);
		if (type == null) {
			throw new TErrorCode(t, TFmt.undefined_type__YY0, t.getString());
		}
		return new TTypeCode(type);
	}

	public Ty parseType(TEnv env, Tree<?> t) {
		String name = t.getString();
		Ty ty = env.getType(name);
		if (ty == null) {
			switch (name) {
			case "()":
				return Ty.tVoid;
			case "bool":
			case "boolean":
				return Ty.tBool;
			case "char":
				return Ty.tChar;
			case "int":
			case "int32":
			case "long":
			case "int64":
				return Ty.tInt;
			case "double":
			case "float":
				return Ty.tFloat;
			case "string":
				return Ty.tString;
			case "_":
				return Ty.tThis;
			}
		}
		return ty;
	}
}
