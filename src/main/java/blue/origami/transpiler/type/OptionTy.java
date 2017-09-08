package blue.origami.transpiler.type;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CastCode.TConvTemplate;
import blue.origami.util.StringCombinator;

public class OptionTy extends MonadTy {

	public OptionTy(String name, Ty ty) {
		super(name, ty);
		this.innerTy = ty;
		// assert !(ty instanceof OptionTy);
	}

	@Override
	public boolean isOption() {
		return true;
	}

	@Override
	public boolean hasVar() {
		return this.innerTy.hasVar();
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		Ty inner = this.innerTy.dupVar(dom);
		if (inner != this.innerTy) {
			return Ty.tOption(inner);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isOption()) {
			return this.innerTy.acceptTy(sub, codeTy.getInnerTy(), logs);
		}
		return this.acceptVarTy(sub, codeTy, logs);
	}

	@Override
	public Ty finalTy() {
		if (this.innerTy.isOption()) {
			return this.innerTy.real().finalTy();
		}
		Ty ty = this.innerTy.finalTy();
		if (this.innerTy != ty) {
			return Ty.tOption(ty);
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.mapType("Option", this.innerTy);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("Option[");
		StringCombinator.append(sb, this.innerTy);
		sb.append("]");
	}

	@Override
	public int costMapTo(TEnv env, Ty ty) {
		if (ty.isOption()) {
			if (this.getInnerTy().isAnyRef() || ty.getInnerTy().isAnyRef()) {
				return CastCode.BESTCAST;
			}
		}
		return CastCode.STUPID;
	}

	@Override
	public Template findMapTo(TEnv env, Ty ty) {
		if (ty.isOption()) {
			if (this.getInnerTy().isAnyRef() || ty.getInnerTy().isAnyRef()) {
				return new TConvTemplate("anycast", this, ty, CastCode.BESTCAST, "%s");
			}
		}
		return null;
	}

	// let f = conv(Option[a] x, f: a->b) : Option[b] {
	// \x x.map(f)
	// }

}