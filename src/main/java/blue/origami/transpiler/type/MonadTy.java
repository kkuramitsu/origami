package blue.origami.transpiler.type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import blue.origami.util.OConsole;
import blue.origami.util.OStrings;

public class MonadTy extends Ty {
	protected String name;
	protected Ty innerTy;

	public MonadTy(String name, Ty ty) {
		this.name = name;
		this.innerTy = ty;
	}

	@Override
	public boolean isNonMemo() {
		return this.innerTy.isNonMemo();
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

	@Override
	public boolean isMutable() {
		return this.name.endsWith("'") || this.getInnerTy().isMutable();
	}

	@Override
	public Ty toImmutable() {
		String name = this.isMutable() ? this.name.substring(0, this.name.length() - 1) : this.name;
		return Ty.tMonad(name, this.getInnerTy().toImmutable());
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
	public Ty dupVar(VarDomain dom) {
		Ty inner = this.innerTy.dupVar(dom);
		if (inner != this.innerTy) {
			return Ty.tMonad(this.name, inner);
		}
		return this;
	}

	@Override
	public Ty finalTy() {
		Ty ty = this.innerTy.finalTy();
		if (this.innerTy != ty) {
			return Ty.tMonad(this.name, ty);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isMonad(this.name)) {
			return this.innerTy.acceptTy(false, codeTy.getInnerTy(), logs);
		}
		return this.acceptVarTy(sub, codeTy, logs);
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType(this.name, this.innerTy);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
		sb.append("[");
		OStrings.append(sb, this.innerTy);
		sb.append("]");
	}

}
