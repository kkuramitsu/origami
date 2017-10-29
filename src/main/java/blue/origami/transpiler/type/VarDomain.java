package blue.origami.transpiler.type;

import blue.origami.common.OStrings;
import blue.origami.transpiler.code.CastCode;

public class VarDomain implements OStrings {

	private Ty[] dom;
	private String[] names;
	private int len = 0;

	private VarDomain(int n) {
		this.dom = new Ty[n];
		this.names = new String[n];
		this.len = 0;
	}

	public <T> VarDomain(T[] a) {
		this(a.length + 4);
	}

	private boolean toParamVar = false;

	public void useParamVar() {
		this.len = 0;
		this.toParamVar = true;
	}

	public Ty conv(Ty ty) {
		return ty.dupVar(this);
	}

	public Ty[] conv(Ty[] ts) {
		Ty[] p = new Ty[ts.length];
		for (int i = 0; i < ts.length; i++) {
			p[i] = this.conv(ts[i]);
		}
		return p;
	}

	public Ty[] matched(Ty[] ts, Ty[] codeTy) {
		Ty[] p = this.conv(ts);
		if (codeTy != null) {
			for (int i = 0; i < ts.length; i++) {
				// ODebug.trace("[%d] %s as %s %s", i, codeTy[i], dParamTypes[i],
				// gParamTypes[i]);
				p[i].acceptTy(true, codeTy[i], VarLogger.Update);
			}
			for (int i = 0; i < ts.length; i++) {
				p[i] = p[i].memoed();
			}
		}
		return p;
	}

	private Ty find(String key) {
		for (int i = 0; i < this.names.length; i++) {
			if (key.equals(this.names[i])) {
				return this.dom[i];
			}
		}
		return null;
	}

	Ty convToVar(VarParamTy paramTy) {
		if (!this.toParamVar) {
			String key = paramTy.getId();
			Ty ty = this.find(key);
			if (ty != null) {
				return ty;
			}
			ty = new VarTy(key);
			this.dom[this.len] = ty;
			this.names[this.len] = key;
			this.len++;
			return ty;
		}
		// assert (paramTy == null) : "never happen at " + paramTy;
		return paramTy;
	}

	Ty convToParam(VarTy varTy) {
		if (this.toParamVar) {
			String key = varTy.getId();
			Ty ty = this.find(key);
			if (ty != null) {
				return ty;
			}
			ty = Ty.tVarParam[this.len];
			this.dom[this.len] = ty;
			this.names[this.len] = key;
			this.len++;
			return ty;
		}
		// assert (varTy == null) : "never happen at " + varTy;
		return varTy;
	}

	// private Ty newVarTy(String name, int id) {
	// String var = String.valueOf((char) ('a' + this.len));
	// Ty ty = this.toParamVar ? Ty.tVarParam[this.len] : new VarTy(var, id);
	// this.dom[this.len] = ty;
	// this.names[this.len] = name == null ? var : name;
	// this.len++;
	// return ty;
	// }
	//
	// public Ty[] paramTypes(String[] names, Ty[] paramTypes) {
	// Ty[] p = paramTypes.clone();
	// for (int i = 0; i < paramTypes.length; i++) {
	// p[i] = p[i].dupVar(this);
	// }
	// return p;
	// }
	//
	// public Ty retType(Ty retType) {
	// return retType.dupVar(this);
	// }
	//
	// public Ty resolvedAt(int index) {
	// return this.dom[index].finalTy();
	// }

	// public static Ty newVarTy(VarDomain dom, String name) {
	// return dom == null ? Ty.tAny : dom.newVarTy(name);
	// }
	//
	// public Ty newVarTy(String name) {
	// String n = this.toParamVar ? name : NameHint.safeName(name);
	// // ODebug.trace("%s -> %s", name, n);
	// for (int i = 0; i < this.len; i++) {
	// if (this.names[i].equals(n)) {
	// return this.dom[i];
	// }
	// }
	// return this.newVarTy(n, -1);
	// }
	//
	// public Ty[] dupParamTypes(Ty[] g) {
	// Ty[] v = new Ty[g.length];
	// for (int i = 0; i < g.length; i++) {
	// v[i] = g[i].dupVar(this);
	// }
	// return v;
	// }

	// public Ty dupRetType(Ty ret) {
	// return ret.dupVar(this);
	// }

	public int mapCost() {
		int mapCost = 0;
		for (int i = 0; i < this.len; i++) {
			Ty ty = this.dom[i].memoed();
			if (ty == this.dom[i]) {
				mapCost += CastCode.STUPID;
			}
			if (ty == Ty.tBool || ty == Ty.tInt || ty == Ty.tFloat) {
				mapCost += CastCode.CAST;
			}
		}
		return mapCost;
	}

	public int usedVars() {
		return this.len;
	}

	// static VarTy[] memoed = { new VarTy("a", 0), new VarTy("b", 1), new
	// VarTy("c", 2), new VarTy("d", 3),
	// new VarTy("e", 4), new VarTy("f", 5), };
	//
	// public static VarTy var(int n) {
	// return memoed[n];
	// }
	//
	// public static Ty rename(Ty t) {
	// VarDomain dom = new VarDomain(Ty.tVarParam);
	// dom.useParamVar();
	// return dom.conv(t);
	// }

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.forEach(sb, 0, this.len, "{", ",", "}", (n) -> {
			OStrings.appendQuoted(sb, this.names[n]);
			sb.append(":");
			OStrings.append(sb, this.dom[n]);
		});
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

}
