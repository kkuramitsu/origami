package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class ClassType implements ParseRule {
	@Override
	public Code apply(TEnv env, AST t) {
		Ty type = this.parseType(env, t);
		if (type == null) {
			throw new ErrorCode(t, TFmt.undefined_type__YY1, t.getString());
		}
		return new TypeCode(type);
	}

	public Ty parseType(TEnv env, AST t) {
		String name = t.getString();
		Ty ty = env.getType(name);
		if (ty == null) {
			switch (name) {
			case "()":
				return Ty.tVoid;
			case "bool":
			case "boolean":
				return Ty.tBool;
			case "byte":
				return Ty.tByte;
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
