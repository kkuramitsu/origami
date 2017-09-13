package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.rule.IndexExpr.GetIndexCode;
import blue.origami.util.ODebug;

public class AssignExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		if (left instanceof GetCode) {
			return new SetCode(((GetCode) left).args()[0], ((GetCode) left).getSource(), right);
		}
		if (left instanceof GetIndexCode) {
			return new ExprCode("[]=", ((GetIndexCode) left).recv, ((GetIndexCode) left).index, right);
		}
		ODebug.log(() -> {
			ODebug.p("No Assignment %s %s", left.getClass().getSimpleName(), left);
		});
		throw new ErrorCode(t.get(_right), TFmt.no_more_assignment);
	}

}