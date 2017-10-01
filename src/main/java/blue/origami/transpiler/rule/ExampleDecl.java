package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;

public class ExampleDecl implements ParseRule, Symbols {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, "main");
		env.getTranspiler().addExample(name, t.get(_body));
		return new DoneCode();
	}

}
