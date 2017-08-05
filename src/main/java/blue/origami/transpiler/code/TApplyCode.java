package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFuncType;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TApplyCode extends CodeN {
	private TType typed = TType.tUntyped;

	public TApplyCode(TCode... values) {
		super(values);
	}

	@Override
	public TType getType() {
		return this.typed;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		if (this.typed.isUntyped()) {
			this.args[0] = this.args[0].asType(env, TType.tUntyped);
			if (this.args[0].getType() instanceof TFuncType) {
				TFuncType funcType = (TFuncType) this.args[0].getType();
				TType[] p = funcType.getParamTypes();
				if (p.length + 1 != this.args.length) {
					throw new TErrorCode("mismatched parameter size %d %d", p.length, this.args.length);
				}
				for (int i = 0; i < p.length; i++) {
					this.args[i + 1] = this.args[i + 1].asType(env, p[i]);
					if (this.args[i + 1].getType().isUntyped()) {
						return this;
					}
				}
				this.typed = funcType.getReturnType();
				return super.asType(env, t);
			}
		}
		return this;
	}

	@Override
	public Template getTemplate(TEnv env) {
		// TODO Auto-generated method stub
		return Template.Null;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushApply(env, this);
	}

}
