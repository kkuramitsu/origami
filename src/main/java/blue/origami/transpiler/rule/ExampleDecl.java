package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDeclCode;

public class ExampleDecl implements ParseRule, OSymbols {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, "main");
		env.getTranspiler().addExample(name, t.get(_body));
		return new TDeclCode();
	}

}
