package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TDeclCode extends TypedCode0 {

	public TDeclCode() {
		super(TType.tVoid);
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return Template.Null;
	}

	@Override
	public String strOut(TEnv env) {
		return "";
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
	}

}
