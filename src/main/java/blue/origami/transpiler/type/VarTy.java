package blue.origami.transpiler.type;

import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OStrings;

public class VarTy extends Ty {
	private static int seq = 27;

	private String name;
	final int varId;
	Ty inferredTy;

	boolean hasMutation = false;

	VarTy(String varName) {
		this.name = varName;
		this.inferredTy = null;
		this.varId = seq++;
		assert (seq > 0);
	}

	public String getId() {
		return this.name + Memo.NonChar + this.varId;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String keyMemo() {
		if (this.inferredTy != null) {
			return this.inferredTy.keyMemo();
		}
		return this.getId();
	}

	@Override
	public String keyFrom() {
		return this.inferredTy == null ? "a" : this.inferredTy.keyMemo();
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		if (this.inferredTy != null) {
			return this.inferredTy.newGeneric(paramTy);
		}
		return new GenericTy(this, paramTy);
	}

	@Override
	public Ty getParamType() {
		return this.inferredTy == null ? this : this.inferredTy.getParamType();
	}

	@Override
	public boolean hasMutation() {
		return this.hasMutation || (this.inferredTy != null && this.inferredTy.hasMutation());
	}

	@Override
	public void foundMutation() {
		this.hasMutation = true;
		if (this.inferredTy != null) {
			this.inferredTy.foundMutation();
		}
	}

	@Override
	public boolean isMutable() {
		return this.inferredTy == null ? this.hasMutation : this.inferredTy.isMutable();
	}

	@Override
	public Ty toImmutable() {
		if (this.inferredTy != null) {
			this.inferredTy = this.inferredTy.toImmutable();
		}
		return this;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.isVar() || this.inferredTy == null || this.inferredTy.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this.inferredTy == null ? dom.convToParam(this) : this.inferredTy.dupVar(dom);
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		return this.inferredTy == null ? this : this.inferredTy.map(f);
	}

	@Override
	public Ty base() {
		return this.inferredTy == null ? this : this.inferredTy.base();
	}

	@Override
	public Ty memoed() {
		return (this.inferredTy == null) ? this : this.inferredTy.memoed();
	}

	private boolean lt(VarTy vt) {
		return this.varId > vt.varId;
	}

	@Override
	public boolean match(boolean sub, Ty codeTy, TypeMatcher logs) {
		// ODebug.trace("%s %s", this, codeTy);
		if (this.inferredTy != null) {
			return this.inferredTy.match(sub, codeTy, logs);
		}
		if (codeTy.isVar()) {
			VarTy varTy = (VarTy) codeTy.base();
			if (varTy.inferredTy != null) {
				return this.match(sub, varTy.inferredTy, logs);
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
		if (this.inferredTy == null) {
			sb.append(this.getId());
		} else {
			sb.append(this.getId());
			sb.append("=");
			OStrings.append(sb, this.inferredTy);
		}
	}

	@Override
	public void typeKey(StringBuilder sb) {
		if (this.inferredTy == null) {
			sb.append("a");
		} else {
			this.inferredTy.typeKey(sb);
		}
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		if (this.inferredTy == null) {
			return codeType.mapType("a");
		} else {
			return this.inferredTy.mapType(codeType);
		}
	}

}
