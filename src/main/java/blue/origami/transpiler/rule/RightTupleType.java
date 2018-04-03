package blue.origami.transpiler.rule;

import blue.origami.common.OArrays;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.TupleTy;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class RightTupleType implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		Ty left = env.parseType(env, t.get(_base), null);
		ParseTree right = t.get(_type);
		if (right.is("RightTupleType")) {
			TupleTy r = (TupleTy) env.parseType(env, right, null);
			return new TypeCode(Ty.tTuple(OArrays.join(Ty[]::new, left, r.getParamTypes())));
		}
		return new TypeCode(Ty.tTuple(left, env.parseType(env, right, null)));
	}
}