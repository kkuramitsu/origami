package blue.origami.transpiler.type;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.NameHint;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class VarTy extends Ty {
	private static int seq = 0;
	private String varName;
	Ty innerTy;
	final int id;
	private Tree<?> s;

	public VarTy(String varName, Tree<?> s) {
		this.varName = varName;
		this.innerTy = null;
		this.id = seq++;
		this.s = s;
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
	public boolean isVarRef() {
		return this.innerTy == null || this.innerTy.isVarRef();
	}

	@Override
	public boolean hasVar() {
		return this.innerTy == null || this.innerTy.hasVar();
	}

	@Override
	public Ty dupVarType(VarDomain dom) {
		return this.innerTy == null ? VarDomain.newVarType(dom, this.varName) : this.innerTy.dupVarType(dom);
	}

	@Override
	public boolean isDynamic() {
		return this.innerTy == null ? true : this.innerTy.isDynamic();
	}

	@Override
	public Ty staticTy() {
		return this.innerTy == null ? this : this.innerTy.staticTy();
	}

	private boolean lt(VarTy vt) {
		return this.id < vt.id;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (this.innerTy != null) {
			return this.innerTy.acceptTy(sub, codeTy, logs);
		}
		if (codeTy instanceof VarTy) {
			VarTy vt = ((VarTy) codeTy);
			if (vt.innerTy != null) {
				return this.acceptTy(sub, vt.innerTy, logs);
			}
			if (this.id != vt.id) {
				return this.lt(vt) ? logs.update(vt, this) : logs.update(this, vt);
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
	public <C> C mapType(TypeMap<C> codeType) {
		if (this.innerTy == null) {
			return codeType.mapType("a");
		} else {
			return this.innerTy.mapType(codeType);
		}
	}

}