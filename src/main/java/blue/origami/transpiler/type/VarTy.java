package blue.origami.transpiler.type;

import blue.origami.transpiler.NameHint;
import blue.origami.util.OStrings;

public class VarTy extends Ty {
	private static int seq = 27;
	private String name;
	Ty resolvedTy;
	final int id;
	boolean hasMutation = false;

	public VarTy(String varName, int id) {
		this.name = varName;
		this.resolvedTy = null;
		this.id = id < 0 ? seq++ : id;
		assert (seq > 0);
	}

	public boolean isParameter() {
		return (this.name != null && NameHint.isOneLetterName(this.name));
	}

	public String getId() {
		if (this.name == null) {
			return "#" + this.id;
		}
		return this.id < 27 ? this.name : this.name + "#" + this.id;
	}

	public String getName() {
		if (this.name == null) {
			return "?";
		}
		return this.id < 27 ? this.name : this.name + "#" + this.id;
	}

	@Override
	public boolean isNonMemo() {
		return this.id > 26;
	}

	@Override
	public Ty real() {
		return this.resolvedTy == null ? this : this.resolvedTy.real();
	}

	@Override
	public Ty getInnerTy() {
		return this.resolvedTy == null ? this : this.resolvedTy.getInnerTy();
	}

	@Override
	public boolean hasMutation() {
		return this.hasMutation || (this.resolvedTy != null && this.resolvedTy.hasMutation());
	}

	@Override
	public void hasMutation(boolean b) {
		this.hasMutation = b;
		if (this.resolvedTy != null) {
			this.resolvedTy.hasMutation(b);
		}
	}

	@Override
	public boolean isMutable() {
		return this.resolvedTy == null ? this.hasMutation : this.resolvedTy.isMutable();
	}

	@Override
	public Ty toImmutable() {
		if (this.resolvedTy != null) {
			this.resolvedTy = this.resolvedTy.toImmutable();
		}
		return this;
	}

	@Override
	public boolean hasVar() {
		return this.isVar() || this.resolvedTy == null || this.resolvedTy.hasVar();
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this.resolvedTy == null ? VarDomain.newVarTy(dom, this.getId()) : this.resolvedTy.dupVar(dom);
	}

	@Override
	public Ty finalTy() {
		return (this.resolvedTy == null) ? this : this.resolvedTy.finalTy();
	}

	private boolean lt(VarTy vt) {
		return this.id > vt.id;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		// ODebug.trace("%s %s", this, codeTy);
		if (this.resolvedTy != null) {
			return this.resolvedTy.acceptTy(sub, codeTy, logs);
		}
		if (codeTy.isVar()) {
			VarTy varTy = (VarTy) codeTy.real();
			if (varTy.resolvedTy != null) {
				return this.acceptTy(sub, varTy.resolvedTy, logs);
			}
			if (this.id != varTy.id) {
				return this.lt(varTy) ? logs.updateVar(varTy, this) : logs.updateVar(this, varTy);
			}
			return true;
		}
		if (logs.updateVar(this, codeTy) && this.name != null) {
			// ODebug.log(() -> ODebug.trace("type inferencing.. %s as %s",
			// this.getName(), codeTy));
		}
		return true;
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.resolvedTy == null) {
			sb.append(this.getName());
		} else {
			sb.append("|");
			sb.append(this.getName());
			sb.append("=");
			OStrings.append(sb, this.resolvedTy);
			sb.append("|");
		}
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		if (this.resolvedTy == null) {
			return codeType.mapType("a");
		} else {
			return this.resolvedTy.mapType(codeType);
		}
	}

}