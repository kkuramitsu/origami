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

	public static Ty eliminateVar(Ty ty) {
		if (!ty.isMemoed()) {
			VarDomain dom = new VarDomain(10);
			dom.useParamVar();
			return dom.conv(ty);
		}
		return ty;
	}

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
