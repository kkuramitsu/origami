package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public interface ParseRule {
	Code apply(Env env, ParseTree t);
}
