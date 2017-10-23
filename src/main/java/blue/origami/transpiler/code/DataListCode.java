package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.ListTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;

public class DataListCode extends DataCode {

	public DataListCode(ListTy dt) {
		super(dt);
	}

	public DataListCode(boolean isMutable, Code... values) {
		super(isMutable, OArrays.emptyNames, values);
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			Ty firstType = this.guessInnerType(ret);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
			}
			this.setType(this.isMutable() ? Ty.tArray(firstType) : Ty.tList(firstType));
			// ODebug.trace("first %s %s", firstType, this.getType());
		}
		if (ret.isList()) {
			Ty ty = ret.getInnerTy();
			if (!this.getType().getInnerTy().acceptTy(bEQ, ty, VarLogger.Update)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(this.isMutable() ? Ty.tArray(ty) : Ty.tList(ty));
		}
		return this.castType(env, ret);
	}

	private Ty guessInnerType(Ty t) {
		if (t.isList()) {
			return t.getInnerTy();
		}
		return Ty.tUntyped();
	}

}