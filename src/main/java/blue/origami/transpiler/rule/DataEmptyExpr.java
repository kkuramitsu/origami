package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataEmptyCode;

public class DataEmptyExpr extends LoggerRule implements Symbols, ParseRule {
  @Override
	public Code apply(Env env, AST t) {
		return new DataEmptyCode();
	}
}
