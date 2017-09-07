package blue.origami.transpiler.type;

import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.code.CastCode;

public class VarDomain {
	private VarTy[] dom;
	private int len = 0;

	public VarDomain(int n) {
		this.dom = new VarTy[n];
		this.len = 0;
	}

	public <T> VarDomain(T[] a) {
		this(a.length);
	}

	public static Ty newVarTy(VarDomain dom, String name) {
		return dom == null ? Ty.tAnyRef : dom.newVarTy(name);
	}

	public VarTy newVarTy() {
		VarTy ty = Ty.tVar(String.valueOf((char) ('a' + this.len)));
		this.dom[this.len] = ty;
		this.len++;
		return ty;
	}

	public Ty[] paramTypes(String[] names, Ty[] paramTypes) {
		Ty[] p = paramTypes.clone();
		for (int i = 0; i < paramTypes.length; i++) {
			if (p[i].isAnyRef()) {
				p[i] = this.newVarTy();
			}
		}
		return p;
	}

	public Ty retType(Ty retType) {
		return retType.isAnyRef() ? this.newVarTy() : retType;
	}

	public Ty resolvedAt(int index) {
		return this.dom[index].finalTy();
	}

	public void rename() {
		char c = 'a';
		for (int i = 0; i < this.len; i++) {
			Ty ty = this.dom[i].finalTy();
			if (ty == this.dom[i]) {
				this.dom[i].rename(String.valueOf(c++));
			}
		}
	}

	public Ty newVarTy(String name) {
		String n = NameHint.safeName(name);
		for (int i = 0; i < this.len; i++) {
			if (n.equals(this.dom[i].getName())) {
				return this.dom[i];
			}
		}
		VarTy ty = Ty.tVar(n);
		this.dom[this.len] = ty;
		this.len++;
		return ty;
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

}
