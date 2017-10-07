package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.StringCode;
import blue.origami.util.OStringUtils;

public class StringExpr implements ParseRule {

	@Override
	public Code apply(TEnv env, AST t) {
		return new StringCode(OStringUtils.unquoteString(t.getString()));
	}

}