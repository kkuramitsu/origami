package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class StateType implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String name = Ty.parseStateName(t.get(_base));
		Ty ty = env.parseType(env, t.get(_param), null);
		return new TypeCode(Ty.tState(name, ty));
	}
}