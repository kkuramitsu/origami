package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;

public class SimpleTy extends Ty {
	protected String name;
	private boolean isBase;

	public SimpleTy(String name) {
		this.name = name;
		this.isBase = false;
	}

	public SimpleTy(String name, int paramSize) {
		this.name = name;
		this.isBase = paramSize > 0;
	}

	@Override
	public String keyMemo() {
		return this.name;
	}

	@Override
	public String keyFrom() {
		return this.keyMemo();
	}

	@Override
	public boolean isMutable() {
		return this.name.startsWith(Ty.Mut);
	}

	@Override
	public Ty toMutable() {
		if (!this.isMutable()) {
			return Ty.t(Ty.Mut + this.name);
		}
		return this;
	}

	@Override
	public Ty toImmutable() {
		if (this.isMutable()) {
			return Ty.t(this.name.substring(Ty.Mut.length()));
		}
		return this;
	}

	public boolean isBase() {
		return this.isBase;
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		if (this.isBase()) {
			return new GenericTy(this, paramTy);
		}
		return this;
	}

	@Override
	public Code getDefaultValue() {
		return null;
	}

	@Override
	public boolean match(boolean sub, Ty codeTy, TypeMatcher logs) {
		if (codeTy.isVar()) {
			return (codeTy.match(false, this, logs));
		}
		return this == codeTy.base();
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
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.mapType(this.name);
	}

}

class AnyRefTy extends SimpleTy {

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