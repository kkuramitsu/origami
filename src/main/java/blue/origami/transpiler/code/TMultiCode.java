package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TMultiCode extends MultiCode {

	public TMultiCode(TCode... nodes) {
		super(nodes);
	}

	@Override
	public TType getType() {
		return this.args[this.args.length - 1].getType();
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		final int last = this.args.length - 1;
		for (int i = 0; i < last; i++) {
			this.args[i] = this.args[i].asType(env, TType.tVoid);
		}
		this.args[last] = this.args[last].asType(env, t);
		return this;
	}

	public boolean isReturnable() {
		return this.args[this.args.length - 1] instanceof TReturnCode;
	}

	public void setReturn() {
		if (!this.isReturnable()) {
			this.args[this.args.length - 1] = new TReturnCode(this.args[this.args.length - 1]);
		}
	}

	@Override
	public Template getTemplate(TEnv env) {
		return Template.Null;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushMulti(env, this);
	}

}
