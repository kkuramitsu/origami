package blue.origami.transpiler;

import java.util.HashMap;
import java.util.function.Supplier;

public abstract class CodeType<C> {
	protected TEnv env;
	protected HashMap<String, C> typeMap = new HashMap<>();

	public CodeType(TEnv env) {
		this.env = env;
	}

	public int seq() {
		return this.typeMap.size();
	}

	/* Properties */

	protected boolean isDyLang;

	public void initProperties() {
		this.isDyLang = this.env.getSymbolOrElse("Int", null) == null;
	}

	public abstract C type(Ty ty);

	public abstract C[] types(Ty... ty);

	public void reg(String key, C c) {
		this.typeMap.put(key, c);
	}

	public C reg(String key, Supplier<C> f) {
		C c = this.typeMap.get(key);
		if (c == null) {
			c = f.get();
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected void reg(Ty t, C c) {
		this.typeMap.put(t.key(), c);
	}

	// public abstract String unique(C c);

	public C mapType(String name) {
		C c = this.typeMap.get(name);
		if (c == null) {
			return this.mapDefaultType(name);
		}
		return c;
	}

	protected abstract C mapDefaultType(String name);

	public abstract String key(C c);

	public C mapType(String prefix, Ty ty) {
		C inner = this.type(ty);
		String key = prefix + this.key(inner);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.mapDefaultType(prefix, ty, inner);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract C mapDefaultType(String prefix, Ty ty, C inner);

	public final C mapType(FuncTy funcTy) {
		String key = this.key(funcTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.gen(funcTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String key(FuncTy funcTy);

	protected abstract C gen(FuncTy funcTy);

	public final C mapType(DataTy dataTy) {
		String key = this.key(dataTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.gen(dataTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String key(DataTy dataTy);

	protected abstract C gen(DataTy dataTy);

	public final Ty fieldTy(String name) {
		NameHint hint = this.env.findGlobalNameHint(this.env, name);
		return hint.getType();
	}

}