package blue.origami.transpiler.code;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class NoneCode extends CommonCode implements ValueCode {

	public NoneCode(AST s) {
		super(Ty.tOption(Ty.tUntyped(s)));
		this.setSource(s);
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		sec.pushNone(env, this);
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.TypeAnnotation_(this.getType(), () -> {
			sh.Keyword("None");
		});
	}

}