package blue.origami.transpiler.type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import blue.origami.transpiler.VarDomain;
import blue.origami.util.OConsole;
import blue.origami.util.StringCombinator;

public class MonadTy extends Ty {
	protected String name;
	protected Ty innerTy;

	public MonadTy(String name, Ty ty) {
		this.name = name;
		this.innerTy = ty;
	}

	public Ty newType(String name, Ty ty) {
		try {
			Constructor<?> c = this.getClass().getConstructor(String.class, Ty.class);
			return (Ty) c.newInstance(name, ty);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			OConsole.exit(1, e);
			return null;
		}
	}

	public boolean equalsName(String name) {
		return this.name.equals(name);
	}

	public boolean isMutable() {
		return this.name.endsWith("'");
	}

	@Override
	public Ty getInnerTy() {
		return this.innerTy;
	}

	@Override
	public boolean hasVar() {
		return this.innerTy.hasVar();
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		Ty inner = this.innerTy.dupTy(dom);
		if (inner != this.innerTy) {
			return Ty.tMonad(this.name, inner);
		}
		return this;
	}

	@Override
	public boolean isDynamic() {
		return this.innerTy.isDynamic();
	}

	@Override
	public Ty nomTy() {
		Ty ty = this.innerTy.nomTy();
		if (this.innerTy != ty) {
			return Ty.tMonad(this.name, ty);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, boolean updated) {
		if (codeTy instanceof MonadTy && ((MonadTy) codeTy).equalsName(this.name)) {
			return this.innerTy.acceptTy(false, codeTy.getInnerTy(), updated);
		}
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, updated));
		}
		return false;
	}

	@Override
	public boolean isUntyped() {
		return this.innerTy.isUntyped();
	}

	@Override
	public String key() {
		return this.name + "[" + this.innerTy.key() + "]";
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType(this.name, this.innerTy);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
		sb.append("[");
		StringCombinator.append(sb, this.innerTy);
		sb.append("]");
	}

}
