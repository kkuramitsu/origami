package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.DictTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;
import blue.origami.util.OStrings;

public class DataDictCode extends DataCode {

	public DataDictCode(boolean isMutable, String[] names, Code[] values) {
		super(isMutable, names, values);
	}

	public DataDictCode(DictTy dt) {
		super(dt);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			Ty firstType = this.guessInnerType(ret);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
			}
			this.setType(this.isMutable() ? Ty.tMonad("Dict'", firstType) : Ty.tMonad("Dict", firstType));
		}
		if (ret.isDict()) {
			Ty ty = ret.getInnerTy();
			if (!this.getType().getInnerTy().acceptTy(bEQ, ty, VarLogger.Update)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(ty);
		}
		return this.castType(env, ret);
	}

	private Ty guessInnerType(Ty t) {
		if (t.isDict()) {
			return t.getInnerTy();
		}
		return Ty.tUntyped();
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.args[0]);
		sb.append(this.isMutable() ? "{" : "[");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			OStrings.appendQuoted(sb, this.names[i]);
			sb.append(":");
			OStrings.append(sb, this.args[i]);
		}
		sb.append(this.isMutable() ? "}" : "]");
	}

}