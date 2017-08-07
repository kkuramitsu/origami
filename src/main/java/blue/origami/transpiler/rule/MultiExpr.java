package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.MultiCode;

public class MultiExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		// if (t.size() == 0) {
		// return new DefaultValueCode(env);
		// }
		Code[] nodes = new Code[t.size()];
		int last = t.size();
		for (int i = 0; i < last; i++) {
			nodes[i] = env.parseCode(env, t.get(i));
		}
		// if (last >= 0) {
		// nodes[last] = this.typeExpr(env, t.get(last));
		// }
		return new MultiCode(false, nodes);
	}
}
