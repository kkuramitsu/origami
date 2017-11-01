package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OStrings;

public class VarTy extends Ty {
	private static int seq = 27;

	private String name;
	final int varId;
	Ty resolvedTy;

	boolean hasMutation = false;

	VarTy(String varName) {
		this.name = varName;
		this.resolvedTy = null;
		this.varId = seq++;
		assert (seq > 0);
	}

	// public boolean isParameter() {
	// return (this.name != null && NameHint.isOneLetterName(this.name));
	// }

	public String getId() {
		return this.name + Memo.NonChar + this.varId;
	}

	// public String getName() {
	// if (this.name == null) {
	// return "?";
	// }
	// return this.varId < 27 ? this.name : this.name + Memo.NonId + this.varId;
	// }

	@Override
	public String keyMemo() {
		if (this.resolvedTy != null) {
			return this.resolvedTy.keyMemo();
		}
		return this.getId();
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		if (this.resolvedTy != null) {
			return this.resolvedTy.newGeneric(paramTy);
		}
		return new GenericTy(this, paramTy);
	}

	@Override
	public Ty getParamType() {
		return this.resolvedTy == null ? this : this.resolvedTy.getParamType();
	}

	@Override
	public boolean hasMutation() {
		return this.hasMutation || (this.resolvedTy != null && this.resolvedTy.hasMutation());
	}

	@Override
	public void foundMutation() {
		this.hasMutation = true;
		if (this.resolvedTy != null) {
			this.resolvedTy.foundMutation();
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
	public boolean hasSome(Predicate<Ty> f) {
		return this.isVar() || this.resolvedTy == null || this.resolvedTy.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this.resolvedTy == null ? dom.convToParam(this) : this.resolvedTy.dupVar(dom);
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		return this.resolvedTy == null ? this : this.resolvedTy.map(f);
	}

	@Override
	public Ty base() {
		return this.resolvedTy == null ? this : this.resolvedTy.base();
	}

	@Override
	public Ty memoed() {
		return (this.resolvedTy == null) ? this : this.resolvedTy.memoed();
	}

	private boolean lt(VarTy vt) {
		return this.varId > vt.varId;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		// ODebug.trace("%s %s", this, codeTy);
		if (this.resolvedTy != null) {
			return this.resolvedTy.acceptTy(sub, codeTy, logs);
		}
		if (codeTy.isVar()) {
			VarTy varTy = (VarTy) codeTy.base();
			if (varTy.resolvedTy != null) {
				return this.acceptTy(sub, varTy.resolvedTy, logs);
			}
			if (this.varId != varTy.varId) {
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
			sb.append(this.getId());
		} else {
			sb.append(this.getId());
			sb.append("=");
			OStrings.append(sb, this.resolvedTy);
		}
	}

	@Override
	public void typeKey(StringBuilder sb) {
		if (this.resolvedTy == null) {
			sb.append("a");
		} else {
			this.resolvedTy.typeKey(sb);
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