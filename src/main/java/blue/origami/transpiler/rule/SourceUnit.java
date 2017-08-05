package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TSourceCode;

public class SourceUnit implements ParseRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode[] nodes = new TCode[t.size()];
		int last = t.size();
		for (int i = 0; i < last; i++) {
			nodes[i] = env.parseCode(env, t.get(i));
		}
		return new TSourceCode(nodes);
	}

}
