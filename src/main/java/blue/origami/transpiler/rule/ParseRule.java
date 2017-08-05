package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;

public interface ParseRule {
	TCode apply(TEnv env, Tree<?> t);
}
