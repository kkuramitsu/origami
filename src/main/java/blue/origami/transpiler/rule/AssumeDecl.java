package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.type.Ty;

public class AssumeDecl implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, AST t) {
		for (AST sub : t.get(_body)) {
			Ty ty = env.parseType(env, sub.get(_type), null);
			for (AST ns : sub.get(_name)) {
				NameHint.addNameHint(env, ns, ty);
				// System.out.println("defined " + ns.getString() + " " +
				// env.findNameHint(ns.getString()));
			}
		}
		return new DoneCode();
	}

}
