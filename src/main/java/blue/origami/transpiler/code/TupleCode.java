package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.TupleTy;
import blue.origami.transpiler.type.Ty;

public class TupleCode extends CodeN {

	public TupleCode(Code[] values) {
		super(values);
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (ret.isTuple()) {
			TupleTy ty = (TupleTy) ret.real();
			if (ty != this.getType()) {
				Ty[] ts = ty.getParamTypes();
				if (ts.length != this.args.length) {
					throw new ErrorCode(this, TFmt.bad_tuple__YY1, ret);
				}
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].bindAs(env, ts[i]);
				}
				this.setType(ty);
			}
			return this;
		}
		if (this.isUntyped()) {
			Ty[] ts = new Ty[this.args.length];
			for (int i = 0; i < this.args.length; i++) {
				Ty ty = Ty.tUntyped();
				this.args[i] = this.args[i].bindAs(env, ty);
				ts[i] = this.args[i].getType();
			}
			this.setType(Ty.tTuple(ts));
		}
		return super.castType(env, ret);
	}

	@Override
	public Code bindAs(Env env, Ty ret) {
		return this.asType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushTuple(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Token("(");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sh.Token(",");
			}
			sh.Expr(this.args[i]);
		}
		sh.Token(")");
	}
}