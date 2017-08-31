package blue.origami.transpiler.type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import blue.origami.transpiler.TEnv;
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

	@Override
	public boolean isMutable() {
		return this.name.endsWith("'");
	}

	@Override
	public Ty returnTy(TEnv env) {
		if (this.isMutable()) {
			String name = this.name.substring(0, this.name.length() - 1);
			return Ty.tMonad(name, this.getInnerTy());
		}
		return this;
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
	public Ty dupVarType(VarDomain dom) {
		Ty inner = this.innerTy.dupVarType(dom);
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
	public Ty staticTy() {
		Ty ty = this.innerTy.staticTy();
		if (this.innerTy != ty) {
			return Ty.tMonad(this.name, ty);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy instanceof MonadTy && ((MonadTy) codeTy).equalsName(this.name)) {
			return this.innerTy.acceptTy(false, codeTy.getInnerTy(), logs);
		}
		if (codeTy instanceof VarTy) {
			return (codeTy.acceptTy(false, this, logs));
		}
		return false;
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
