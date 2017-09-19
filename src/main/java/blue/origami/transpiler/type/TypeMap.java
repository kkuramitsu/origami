package blue.origami.transpiler.type;

import java.util.HashMap;
import java.util.function.Supplier;

import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TEnv;

public abstract class TypeMap<C> {
	protected TEnv env;
	protected final HashMap<String, C> typeMap = new HashMap<>();

	public TypeMap(TEnv env) {
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

	public final C mapForeignFuncType(FuncTy funcTy) {
		String key = this.keyForeignFuncType(funcTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.genForeignFuncType(funcTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String keyForeignFuncType(FuncTy funcTy);

	protected abstract C genForeignFuncType(FuncTy funcTy);

	public final C mapForeignTupleType(TupleTy tupleTy) {
		String key = this.keyForeignTupleType(tupleTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.genForeignTupleType(tupleTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String keyForeignTupleType(TupleTy tupleTy);

	protected abstract C genForeignTupleType(TupleTy tupleTy);

	public final C mapForeignDataType(DataTy dataTy) {
		String key = this.keyForeignDataType(dataTy);
		C c = this.typeMap.get(key);
		if (c == null) {
			c = this.genForeingDataType(dataTy);
			this.typeMap.put(key, c);
		}
		return c;
	}

	protected abstract String keyForeignDataType(DataTy dataTy);

	protected abstract C genForeingDataType(DataTy dataTy);

	public final Ty fieldTy(String name) {
		NameHint hint = this.env.findGlobalNameHint(this.env, name);
		return hint.getType();
	}

}