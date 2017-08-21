package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;

public class NoneCode extends CommonCode implements ValueCode {

	public NoneCode(Tree<?> s) {
		super(Ty.tOption(Ty.tUntyped(s)));
		this.setSource(s);
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushNone(env, this);
	}

}