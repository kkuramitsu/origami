package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

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
	public boolean isMutable() {
		return this.name.startsWith(Ty.Mut);
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
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isVar()) {
			return (codeTy.acceptTy(false, this, logs));
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