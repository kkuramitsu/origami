package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.GenericTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMatchContext;

public class ListCode extends CodeN {

	public ListCode(Ty dt) {
		super(dt);
		assert (this.isList(dt));
	}

	public ListCode(boolean isMutable, Code... values) {
		super(values);
	}

	private boolean isList(Ty ty) {
		ty = ty.devar();
		if (ty instanceof GenericTy) {
			Ty base = ((GenericTy) ty).getBaseType().devar();
			return (base == Ty.tList);
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
			this.setType(Ty.tList(firstType));
			// ODebug.trace("first %s %s", firstType, this.getType());
		}
		if (this.isList(ret)) {
			Ty ty = ret.getParamType();
			if (!this.getType().getParamType().match(TypeMatchContext.Update, bEQ, ty)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(Ty.tList(ty));
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
		return Ty.tVar(null);
	}

}