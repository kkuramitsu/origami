package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TArrayCode extends MultiCode {

	public TArrayCode(TCode... values) {
		super(values);
	}

	public TArrayCode(String... values) {
		super(TValueCode.values(values));
	}

	@Override
	public TType getType() {
		return TType.tArray(this.args[0].getType());
	}

	public TType getElementType() {
		return this.args[0].getType();
	}

	@Override
	public Template getTemplate(TEnv env) {
		return Template.Null;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushArray(env, this);
	}

}
