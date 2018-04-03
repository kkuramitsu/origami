package blue.origami.transpiler.rule;

import blue.origami.common.Symbol;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class FuncType implements ParseRule, Symbols {
	public final static Symbol _TupleType = Symbol.unique("TupleType");

	@Override
	public Code apply(Env env, ParseTree t) {
		Ty ret = env.parseType(env, t.get(_type), null);
		if (t.has(_base)) {
			ParseTree from = t.get(_base);
			if (from.is("TupleType")) {
				Ty[] a = env.parseTypes(env, t.get(_base), null);
				return new TypeCode(Ty.tFunc(ret, a));
			} else {
				Ty fromTy = env.parseType(env, t.get(_base), null);
				return new TypeCode(Ty.tFunc(ret, fromTy));
			}
		} else {
			Ty[] a = env.parseTypes(env, t.get(_param), null);
			return new TypeCode(Ty.tFunc(ret, a));
		}
	}
}