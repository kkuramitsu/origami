package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TDeclCode extends TTypedCode {

	public TDeclCode() {
		super(TType.tUnit);
	}

	@Override
	public TTemplate getTemplate(TEnv env) {
		return TTemplate.Null;
	}

	@Override
	public String strOut(TEnv env) {
		return "";
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
	}

}
