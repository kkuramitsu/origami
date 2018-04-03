package blue.origami.transpiler.rule;

import java.util.Arrays;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class TupleType implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		Ty[] a = Arrays.stream(t.asArray()).map(x -> env.parseType(env, x, null)).toArray(Ty[]::new);
		return new TypeCode(Ty.tTuple(a));
	}
}
