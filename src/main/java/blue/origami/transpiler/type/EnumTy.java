package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

//enum Season = Spring|Summer|Fall|Winter
//enum Mutable

public class EnumTy extends Ty {

	String name;
	String[] names;

	@Override
	public boolean eq(Ty ty) {
		Ty right = ty.devar();
		return this == right;
	}

	public boolean isBool() {
		return this.names.length <= 2;
	}

	public boolean valueOf(String name) {
		if (this.names.length == 0) {
			return (name.equals(this.name));
		}
		return (name.equals(this.names[0]));
	}

	public int valueOf2(String name) {
		for (int i = 0; i < this.names.length; i++) {
			if (this.names[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	// @Override
	// public boolean match(TypeMatchContext logs, boolean sub, Ty codeTy) {
	// if (codeTy.devar() == this) {
	// return true;
	// }
	// return this.matchVar(sub, codeTy, logs);
	// }

	@Override
	public Ty memoed() {
		return this;
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
		return Ty.tInt.mapType(codeType);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public String keyOfArrows() {
		return this.name;
	}

}
