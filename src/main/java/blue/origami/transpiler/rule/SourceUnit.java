package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.SourceCode;

public class SourceUnit implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		Code[] nodes = new Code[t.size()];
		int last = t.size();
		for (int i = 0; i < last; i++) {
			nodes[i] = env.parseCode(env, t.get(i));
		}
		return new SourceCode(nodes);
	}

}
