package blue.origami.transpiler.type;

import blue.origami.transpiler.NameHint;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class VarTy extends Ty {
	private static int seq = 27;
	private String varName;
	Ty innerTy;
	final int id;

	public VarTy(String varName, int id) {
		this.varName = varName;
		this.innerTy = null;
		this.id = id < 0 ? seq++ : id;
		assert (seq > 0);
	}

	public boolean isParameter() {
		return (this.varName != null && NameHint.isOneLetterName(this.varName));
	}

	public String getName() {
		if (this.varName == null) {
			return "_" + this.id;
		}
		return this.id < 27 ? this.varName : this.varName + "#" + this.id;
	}

	@Override
	public boolean isNonMemo() {
		return this.id > 26;
	}

	@Override
	public Ty real() {
		return this.innerTy == null ? this : this.innerTy.real();
	}

	@Override
	public Ty getInnerTy() {
		return this.innerTy == null ? this : this.innerTy.getInnerTy();
	}

	@Override
	public Ty toImmutable() {
		if (this.innerTy != null) {
			this.innerTy = this.innerTy.toImmutable();
		}
		return this;
	}

	public void rename(String name) {
		this.varName = name;
	}

	@Override
	public boolean hasVar() {
		return this.isVar() || this.innerTy == null || this.innerTy.hasVar();
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this.innerTy == null ? VarDomain.newVarTy(dom, this.varName) : this.innerTy.dupVar(dom);
	}

	@Override
	public Ty finalTy() {
		return this.innerTy == null ? this : this.innerTy.finalTy();
	}

	private boolean lt(VarTy vt) {
		return this.id > vt.id;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (this.innerTy != null) {
			return this.innerTy.acceptTy(sub, codeTy, logs);
		}
		if (codeTy.isVar()) {
			VarTy varTy = (VarTy) codeTy.real();
			if (varTy.innerTy != null) {
				return this.acceptTy(sub, varTy.innerTy, logs);
			}
			if (this.id != varTy.id) {
				return this.lt(varTy) ? logs.update(varTy, this) : logs.update(this, varTy);
			}
			return true;
		}
		if (logs.update(this, codeTy) && this.varName != null) {
			ODebug.trace("infer %s as %s", this.getName(), codeTy);
		}
		return true;
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.innerTy == null) {
			sb.append(this.getName());
		} else {
			sb.append("|");
			sb.append(this.getName());
			sb.append("=");
			StringCombinator.append(sb, this.innerTy);
			sb.append("|");
		}
	}

	// @Override
	// public String key() {
	// if (this.innerTy == null) {
	// return "a";
	// } else {
	// return this.innerTy.key();
	// }
	// }

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		if (this.innerTy == null) {
			return codeType.mapType("a");
		} else {
			return this.innerTy.mapType(codeType);
		}
	}

}