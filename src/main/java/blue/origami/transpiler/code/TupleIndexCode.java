package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.TupleTy;
import blue.origami.transpiler.type.Ty;

public class TupleIndexCode extends Code1 {

	int index;

	public TupleIndexCode(Code tuple, int index) {
		super(tuple);
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	public Code bindAs(Env env, Ty ret) {
		return this.asType(env, ret);
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			this.inner = this.inner.asType(env, Ty.tUntyped());
			Ty ty = this.inner.getType();
			if (!ty.isTuple()) {
				throw new ErrorCode(this.inner, TFmt.not_tuple);
			}
			TupleTy tupleTy = (TupleTy) ty.real();
			if (!(this.index < tupleTy.getParamSize())) {
				throw new ErrorCode(this.inner, TFmt.bad_tuple__YY1);
			}
			this.setType(tupleTy.getParamTypes()[this.index]);
		}
		return super.castType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushTupleIndex(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "get-" + this.index, this.inner);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Expr(this.inner);
		sh.Token("#" + this.index);
	}

}