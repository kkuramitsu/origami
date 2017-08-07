package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;

public interface ParseRule {
	Code apply(TEnv env, Tree<?> t);
}
