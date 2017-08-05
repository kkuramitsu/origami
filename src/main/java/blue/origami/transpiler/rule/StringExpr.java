package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TStringCode;
import blue.origami.util.OStringUtils;

public class StringExpr implements ParseRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		return new TStringCode(OStringUtils.unquoteString(t.getString()));
	}

}