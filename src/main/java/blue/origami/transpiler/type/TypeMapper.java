package blue.origami.transpiler.type;

import java.util.HashMap;
import java.util.function.Supplier;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;

public abstract class TypeMapper<C> {
	protected Env env;
	protected final HashMap<String, C> typeMap = new HashMap<>();

	public TypeMapper(Env env) {
		this.env = env;
	}

	public Env env() {
		return this.env;
	}

	public void initProperties() {
	}

	public int seq() {
		return this.typeMap.size();
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
		this.typeMap.put(t.toString(), c);
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
		}
		return c;
	}

	protected abstract C mapDefaultType(String prefix, Ty ty, C inner);

	/** FuncType **/

	public final C forFuncType(FuncTy funcTy) {
		String key = this.keyFuncType(funcTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.genFuncType(funcTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String keyFuncType(FuncTy funcTy);

	protected abstract C genFuncType(FuncTy funcTy);

	public final C forTupleType(TupleTy tupleTy) {
		String key = this.keyTupleType(tupleTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.genTupleType(tupleTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String keyTupleType(TupleTy tupleTy);

	protected abstract C genTupleType(TupleTy tupleTy);

	public final C forDataType(DataTy dataTy) {
		String key = this.keyDataType(dataTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.genDataType(dataTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String keyDataType(DataTy dataTy);

	protected abstract C genDataType(DataTy dataTy);

	public final Ty fieldTy(String name) {
		NameHint hint = this.env.findGlobalNameHint(this.env, name);
		return hint.getType();
	}

}
