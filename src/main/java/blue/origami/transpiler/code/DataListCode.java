package blue.origami.transpiler.code;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.ListTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;

public class DataListCode extends DataCode {

	public DataListCode(ListTy dt) {
		super(dt);
	}

	public DataListCode(boolean isMutable, Code... values) {
		super(isMutable, TArrays.emptyNames, values);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			Ty firstType = this.guessInnerType(ret);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
			}
			this.setType(this.isMutable() ? Ty.tList(firstType) : Ty.tImList(firstType));
			// ODebug.trace("first %s %s", firstType, this.getType());
		}
		if (ret.isList()) {
			Ty ty = ret.getInnerTy();
			if (!this.getType().getInnerTy().acceptTy(bEQ, ty, VarLogger.Update)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(this.isMutable() ? Ty.tList(ty) : Ty.tImList(ty));
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