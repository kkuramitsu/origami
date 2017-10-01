package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
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
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
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
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushTupleIndex(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.inner.strOut(sb);
		sb.append("#" + this.index);
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.Expr(this.inner);
		sh.Token("#" + this.index);
	}

}