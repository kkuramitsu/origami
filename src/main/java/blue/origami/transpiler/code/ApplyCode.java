package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;
import blue.origami.util.OStrings;

public class ApplyCode extends CodeN {
	public ApplyCode(Code... values) {
		super(values);
	}

	public ApplyCode(List<Code> l) {
		this(l.toArray(new Code[l.size()]));
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (!this.isUntyped()) {
			return this.castType(env, ret);
		}
		this.args[0] = this.args[0].asType(env, Ty.tUntyped());
		if (this.args[0] instanceof FuncRefCode) {
			// ODebug.trace("switching to expr %s", this.args[0]);
			String name = ((FuncRefCode) this.args[0]).getName();
			return new ExprCode(name, TArrays.ltrim(this.args)).asType(env, ret);
		}

		Ty firstType = this.args[0].getType();
		if (firstType.isFunc()) {
			FuncTy funcType = (FuncTy) firstType.real();
			Ty[] p = funcType.getParamTypes();
			if (p.length + 1 != this.args.length) {
				throw new ErrorCode(TFmt.mismatched_parameter_size_S_S, p.length, this.args.length);
			}
			for (int i = 0; i < p.length; i++) {
				this.args[i + 1] = this.args[i + 1].asType(env, p[i]);
			}
			this.setType(funcType.getReturnType());
			return super.castType(env, ret);
		}
		if (firstType.isVar()) {
			Ty[] p = new Ty[this.args.length - 1];
			for (int i = 1; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, Ty.tUntyped());
				p[i - 1] = this.args[i].getType();
			}
			Ty funcType = Ty.tFunc(ret, p);
			firstType.acceptTy(bSUB, funcType, VarLogger.Update);
			this.setType(ret);
			return this;
		}
		throw new ErrorCode(this.args[0], TFmt.not_function__YY1, this.args[0].getType());
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushApply(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.args[0]);
		sb.append("(");
		OStrings.joins(sb, TArrays.ltrim2(this.args), ",");
		sb.append(")");
	}

}
