package blue.origami.transpiler.type;

import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.code.CastCode;

public class VarDomain {
	private VarTy[] dom;
	private String[] names;
	private int len = 0;

	private VarDomain(int n) {
		this.dom = new VarTy[n];
		this.names = new String[n];
		this.len = 0;
	}

	public <T> VarDomain(T[] a) {
		this(a.length + 4);
	}

	private VarTy newVarTy(String name, int id) {
		String var = String.valueOf((char) ('a' + this.len));
		VarTy ty = this.useMemo ? var(this.len) : new VarTy(var, id);
		this.dom[this.len] = ty;
		this.names[this.len] = name == null ? var : name;
		this.len++;
		return ty;
	}

	public Ty[] paramTypes(String[] names, Ty[] paramTypes) {
		Ty[] p = paramTypes.clone();
		for (int i = 0; i < paramTypes.length; i++) {
			if (p[i].isAnyRef()) {
				p[i] = this.newVarTy(null, -1);
			}
		}
		return p;
	}

	public Ty retType(Ty retType) {
		return retType.isAnyRef()
				? this.newVarTy(null, -1/* Integer.MAX_VALUE */) : retType;
	}

	public Ty resolvedAt(int index) {
		return this.dom[index].finalTy();
	}

	public static Ty newVarTy(VarDomain dom, String name) {
		return dom == null ? Ty.tAnyRef : dom.newVarTy(name);
	}

	public Ty newVarTy(String name) {
		String n = this.useMemo ? name : NameHint.safeName(name);
		for (int i = 0; i < this.len; i++) {
			if (this.names[i].equals(n)) {
				return this.dom[i];
			}
		}
		return this.newVarTy(n, -1);
	}

	public Ty[] dupParamTypes(Ty[] dParamTypes, Ty[] codeTy) {
		Ty[] gParamTypes = new Ty[dParamTypes.length];
		for (int i = 0; i < dParamTypes.length; i++) {
			gParamTypes[i] = dParamTypes[i].dupVar(this);
			if (codeTy != null) {
				gParamTypes[i].acceptTy(true, codeTy[i], VarLogger.Update);
			}
		}
		if (codeTy != null) {
			for (int i = 0; i < dParamTypes.length; i++) {
				gParamTypes[i].acceptTy(true, codeTy[i], VarLogger.Update);
			}
			for (int i = 0; i < dParamTypes.length; i++) {
				gParamTypes[i] = gParamTypes[i].finalTy();
			}
		}
		return gParamTypes;
	}

	public Ty[] dupParamTypes(Ty[] dParamTypes) {
		return this.dupParamTypes(dParamTypes, null);
	}

	public Ty dupRetType(Ty ret) {
		return ret.dupVar(this);
	}

	public int mapCost() {
		int mapCost = 0;
		for (int i = 0; i < this.len; i++) {
			Ty t = this.dom[i].finalTy();
			if (t == this.dom[i]) {
				mapCost += CastCode.STUPID;
			}
			if (t == Ty.tBool || t == Ty.tInt || t == Ty.tFloat) {
				mapCost += CastCode.CAST;
			}
		}
		return mapCost;
	}

	private boolean useMemo = false;

	public void reset() {
		this.len = 0;
		this.useMemo = true;
	}

	static VarTy[] memoed = { new VarTy("a", 0), new VarTy("b", 1), new VarTy("c", 2), new VarTy("d", 3),
			new VarTy("e", 4), new VarTy("f", 5), };

	public static VarTy var(int n) {
		return memoed[n];
	}

}
