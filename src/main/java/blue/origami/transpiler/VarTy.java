package blue.origami.transpiler;

import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class VarTy extends Ty {
	private static int seq = 0;
	private String varName;
	private Ty innerTy;
	final int id;

	public VarTy(String varName) {
		this.varName = varName;
		this.innerTy = null;
		this.id = seq++;
	}

	public boolean isParameter() {
		return (this.varName != null && NameHint.isOneLetterName(this.varName));
	}

	public String getName() {
		return this.varName == null ? "_" + this.id
				: this.varName /* + this.id */;
	}

	@Override
	public Ty type() {
		return this.innerTy == null ? this : this.innerTy.type();
	}

	@Override
	public Ty getInnerTy() {
		return this.innerTy == null ? this : this.innerTy.getInnerTy();
	}

	public void rename(String name) {
		this.varName = name;
	}

	@Override
	public boolean isUntyped() {
		return false; // typed as variable types
	}

	@Override
	public boolean isVarRef() {
		return this.innerTy == null || this.innerTy.isVarRef();
	}

	@Override
	public boolean hasVar() {
		return this.innerTy == null || this.innerTy.hasVar();
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		return this.innerTy == null ? dom.newVarType(this.varName) : this.innerTy.dupTy(dom);
	}

	@Override
	public boolean isDynamic() {
		return this.innerTy == null ? true : this.innerTy.isDynamic();
	}

	@Override
	public Ty nomTy() {
		return this.innerTy == null ? this : this.innerTy.nomTy();
	}

	private boolean lt(VarTy vt) {
		return this.id < vt.id;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, boolean updated) {
		if (this.innerTy == null) {
			if (codeTy instanceof VarTy) {
				VarTy vt = ((VarTy) codeTy);
				if (vt.innerTy != null) {
					return this.acceptTy(sub, vt.innerTy, updated);
				} else {
					if (updated && this.id != vt.id) {
						if (this.lt(vt)) {
							vt.innerTy = this;
						} else {
							this.innerTy = vt;
						}
					}
					return true;
				}
			}
			if (updated) {
				if (this.varName != null) {
					ODebug.trace("infer %s as %s", this.getName(), codeTy);
				}
				assert !(codeTy instanceof DictTy);
				this.innerTy = codeTy;
			}
			return true;
		}
		return this.innerTy.acceptTy(sub, codeTy, updated);
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.innerTy == null) {
			sb.append(this.getName());
		} else {
			StringCombinator.append(sb, this.innerTy);
		}
	}

	@Override
	public String key() {
		if (this.innerTy == null) {
			return "a";
		} else {
			return this.innerTy.key();
		}
	}

	@Override
	public <C> C mapType(CodeType<C> codeType) {
		if (this.innerTy == null) {
			return codeType.mapType("a");
		} else {
			return this.innerTy.mapType(codeType);
		}
	}

	@Override
	public String strOut(TEnv env) {
		return this.innerTy.nomTy().strOut(env);
	}

}