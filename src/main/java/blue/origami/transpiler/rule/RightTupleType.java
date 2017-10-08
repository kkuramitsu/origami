package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.TupleTy;
import blue.origami.transpiler.type.Ty;

public class RightTupleType implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, AST t) {
		Ty left = env.parseType(env, t.get(_base), null);
		AST right = t.get(_type);
		if (right.is("RightTupleType")) {
			TupleTy r = (TupleTy) env.parseType(env, right, null);
			return new TypeCode(Ty.tTuple(TArrays.join(Ty[]::new, left, r.getParamTypes())));
		}
		return new TypeCode(Ty.tTuple(left, env.parseType(env, right, null)));
	}
}