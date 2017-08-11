package blue.origami.transpiler.code;

import blue.origami.transpiler.ArrayTy;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;

public class DataArrayCode extends DataCode {

	public DataArrayCode(boolean isMutable, Code... values) {
		super(isMutable, TArrays.emptyNames, values);
	}

	public DataArrayCode(ArrayTy dt) {
		super(dt.isMutable(), dt);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			Ty firstType = this.guessInnerType(ret);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
			}
			this.setType(this.isMutable() ? Ty.tArray(firstType) : Ty.tImArray(firstType));
			return this;
		}
		if (ret.isArray()) {
			Ty ty = ret.getInnerTy();
			if (!this.getType().getInnerTy().acceptTy(bEQ, ty, bUPDATE)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(ty);
			return this;
		}
		return this.castType(env, ret);
	}

	private Ty guessInnerType(Ty t) {
		if (t.isArray()) {
			return t.getInnerTy();
		}
		return Ty.tUntyped();
	}

}