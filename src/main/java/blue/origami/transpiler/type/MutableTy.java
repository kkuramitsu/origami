package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.transpiler.Env;
import origami.nez2.Token;

class MutableTy extends Ty {
	Ty base;

	MutableTy(Ty base) {
		this.base = base;
	}

	@Override
	public boolean eq(Ty ty) {
		Ty right = ty.devar();
		if (this == right) {
			return true;
		}
		if (right instanceof MutableTy) {
			return ((MutableTy) right).base.eq(this.base);
		}
		return false;
	}

	@Override
	public int paramSize() {
		return this.base.paramSize();
	}

	@Override
	public Ty[] params() {
		return this.base.params();
	}

	@Override
	public Ty param(int n) {
		return this.base.param(n);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.base.strOut(sb);
		sb.append("$");
	}

	@Override
	public String keyOfMemo() {
		return this.base.keyOfMemo() + "$";
	}

	@Override
	public String keyOfArrows() {
		return this.base.keyOfArrows();
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Ty toMutable() {
		return this;
	}

	@Override
	public Ty toImmutable() {
		return this.base.toImmutable();
	}

	@Override
	public boolean hasSuperType(Ty left) {
		return left.eq(this) || this.base.hasSuperType(left);
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return f.test(this) || this.base.hasSome(f);
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		return this.base.map(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return new MutableTy(this.base.dupVar(dom));
	}

	@Override
	public Ty getParamType() {
		return this.base.getParamType();
	}

	@Override
	public Ty resolveFieldType(Env env, Token s, String name) {
		return this.base.resolveFieldType(env, s, name);
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return this.base.mapType(codeType);
	}

}