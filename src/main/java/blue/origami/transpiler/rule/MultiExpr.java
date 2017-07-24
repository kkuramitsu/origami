package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TMultiCode;

public class MultiExpr implements TTypeRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		// if (t.size() == 0) {
		// return new DefaultValueCode(env);
		// }
		TCode[] nodes = new TCode[t.size()];
		int last = t.size();
		for (int i = 0; i < last; i++) {
			nodes[i] = env.typeTree(env, t.get(i));
		}
		// if (last >= 0) {
		// nodes[last] = this.typeExpr(env, t.get(last));
		// }
		return new TMultiCode(nodes);
	}
}
