package blue.origami.transpiler.code;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.FuncTy;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class ApplyCode extends CodeN {
	public ApplyCode(Code... values) {
		super(values);
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		if (this.isUntyped()) {
			this.args[0] = this.args[0].asType(env, Ty.tUntyped);
			if (this.args[0] instanceof FuncRefCode) {
				// ODebug.trace("switching to expr %s", this.args[0]);
				String name = ((FuncRefCode) this.args[0]).name;
				return new ExprCode(name, TArrays.ltrim(this.args)).asType(env, t);
			}
			if (this.args[0].getType() instanceof FuncTy) {
				FuncTy funcType = (FuncTy) this.args[0].getType();
				Ty[] p = funcType.getParamTypes();
				if (p.length + 1 != this.args.length) {
					throw new ErrorCode("mismatched parameter size %d %d", p.length, this.args.length);
				}
				for (int i = 0; i < p.length; i++) {
					this.args[i + 1] = this.args[i + 1].asType(env, p[i]);
					if (this.args[i + 1].isUntyped()) {
						return this.StillUntyped();
					}
				}
				this.setType(funcType.getReturnType());
				return super.asExactType(env, t);
			}
			if (this.args[0].isUntyped()) {
				if (!t.isSpecific()) {
					return this.StillUntyped();
				}
				Ty[] p = new Ty[this.args.length - 1];
				for (int i = 0; i < p.length; i++) {
					this.args[i + 1] = this.args[i + 1].asType(env, Ty.tUntyped);
					if (this.args[i + 1].isUntyped()) {
						return this.StillUntyped();
					}
				}
				Ty funcType = Ty.tFunc(t, p);
				this.args[0] = this.args[0].asType(env, funcType);
				if (this.args[0].getType() == funcType) {
					this.setType(t);
					return super.asExactType(env, t);
				}
				return this.StillUntyped();
			}
			throw new ErrorCode(this.args[0], TFmt.not_function__YY0, this.args[0].getType());
		}
		return this;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushApply(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.args[0]);
		sb.append("(");
		for (int i = 1; i < this.args.length; i++) {
			if (i > 1) {
				sb.append(",");
			}
			StringCombinator.append(sb, this.args[i]);
		}
		sb.append(")");
	}

}
