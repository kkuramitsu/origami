package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.ODebug;

public class TDataCode extends MultiTypedCode {
	private String[] names;

	public TDataCode(String[] names, TCode[] values) {
		super(TType.tData, null, values);
		this.names = names;
	}

	public String[] getNames() {
		return this.names;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("{}");
	}

	@Override
	public String strOut(TEnv env) {
		ODebug.TODO(this);
		return this.getTemplate(env).format();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushData(env, this);
	}

}