package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;

public class BaseTy extends Ty {
	protected String name;

	public BaseTy(String name) {
		this.name = name;
	}

	public BaseTy(String name, int paramSize) {
		this.name = name;
	}

	@Override
	public boolean eq(Ty ty) {
		Ty right = ty.devar();
		if (this == right) {
			return true;
		}
		if (right instanceof BaseTy) {
			return ((BaseTy) right).name.equals(this.name);
		}
		return false;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public String keyOfMemo() {
		return this.name;
	}

	@Override
	public String keyOfArrows() {
		return this.keyOfMemo();
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		return new GenericTy(this, paramTy);
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		return f.apply(this);
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return f.test(this);
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.mapType(this.name);
	}

}

class AnyRefTy extends BaseTy {

	public AnyRefTy(String name) {
		super(name);
	}

	@Override
	public int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return CodeMap.CAST;
	}

	@Override
	public int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return CodeMap.CAST;
	}

	@Override
	public CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return env.getArrow(env, Ty.tAnyRef, Ty.tVarParam[0]);
	}

	@Override
	public CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return env.getArrow(env, Ty.tVarParam[0], Ty.tAnyRef);
	}

}