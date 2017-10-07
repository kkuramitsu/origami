package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Symbol;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class TupleType implements ParseRule, Symbols {
	public final static Symbol _TupleType = Symbol.unique("TupleType");

	@Override
	public Code apply(TEnv env, AST t) {
		Ty[] a = new Ty[t.size()];
		for (int i = 0; i < t.size(); i++) {
			a[i] = env.parseType(env, t.get(i), null);
		}
		return new TypeCode(Ty.tTuple(a));
	}
}