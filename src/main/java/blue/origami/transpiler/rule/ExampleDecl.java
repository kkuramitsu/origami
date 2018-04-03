package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import origami.nez2.ParseTree;

public class ExampleDecl implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, ParseTree t) {
		String name = t.get(_name).asString();
		env.getTranspiler().addExample(name, t.get(_body));
		return new DoneCode();
	}

}
