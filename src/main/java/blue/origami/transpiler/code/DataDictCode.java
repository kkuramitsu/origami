package blue.origami.transpiler.code;

import blue.origami.transpiler.DictTy;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class DataDictCode extends DataCode {

	public DataDictCode(boolean isMutable, String[] names, Code[] values) {
		super(isMutable, names, values);
	}

	public DataDictCode(DictTy dt) {
		super(dt.isMutable(), dt);
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		if (this.isUntyped()) {
			Ty firstType = this.guessInnerType(t);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
			}
			this.setType(this.isMutable() ? Ty.tDict(firstType) : Ty.tImDict(firstType));
			return this;
		}
		if (t.is((dt) -> dt.isDict())) {
			Ty ty = t.getInnerTy();
			if (!this.getType().getInnerTy().acceptTy(bEQ, ty, bUPDATE)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(ty);
			return this;
		}
		return this.castType(env, t);
	}

	private Ty guessInnerType(Ty t) {
		if (t.is((dt) -> dt.isDict())) {
			return t.getInnerTy();
		}
		return Ty.tUntyped();
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.args[0]);
		sb.append(this.isMutable() ? "{" : "[");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			StringCombinator.appendQuoted(sb, this.names[i]);
			sb.append(":");
			StringCombinator.append(sb, this.args[i]);
		}
		sb.append(this.isMutable() ? "}" : "]");
	}

}