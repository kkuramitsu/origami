package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class AssumeDecl implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		for (ParseTree sub : t.get(_body).asArray()) {
			Ty ty = env.parseType(env, sub.get(_type), null);
			for (ParseTree ns : sub.get(_name).asArray()) {
				NameHint.addNameHint(env, env.s(ns), ty);
				// System.out.println("defined " + ns.getString() + " " +
				// env.findNameHint(ns.getString()));
			}
		}
		return new DoneCode();
	}

}
