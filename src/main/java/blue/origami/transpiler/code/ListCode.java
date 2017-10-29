package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.GenericTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;

public class ListCode extends CodeN {
	boolean isMutable = false;

	public ListCode(Ty dt) {
		super(dt);
		assert (this.isList(dt));
	}

	public ListCode(boolean isMutable, Code... values) {
		super(values);
		this.isMutable = isMutable;
	}

	public boolean isMutable() {
		return this.isMutable;
	}

	private boolean isList(Ty ty) {
		ty = ty.base();
		if (ty instanceof GenericTy) {
			Ty base = ((GenericTy) ty).getBaseType().base();
			return (base == Ty.tList || base == Ty.tMList);
		}
		return false;
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
		if (this.isList(ret)) {
			Ty ty = ret.getParamType();
			if (!this.getType().getParamType().acceptTy(bEQ, ty, VarLogger.Update)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(this.isMutable() ? Ty.tArray(ty) : Ty.tList(ty));
		}
		return this.castType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushList(this);
	}

	private Ty guessInnerType(Ty t) {
		if (this.isList(t)) {
			return t.getParamType();
		}
		return Ty.tUntyped();
	}

}