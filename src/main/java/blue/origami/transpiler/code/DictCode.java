package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.GenericTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMatcher;

public class DictCode extends CodeN {
	protected String[] names;
	boolean isMutable = false;

	public DictCode(Ty dt) {
		super(dt, OArrays.emptyCodes);
		this.names = OArrays.emptyNames;
		this.isMutable = dt.isMutable();
		assert (this.isDict(dt));
	}

	public DictCode(boolean isMutable, String[] names, Code[] values) {
		super(values);
		this.names = names;
		this.isMutable = isMutable;
		assert (names.length == values.length);
	}

	public String[] getNames() {
		return this.names;
	}

	public boolean isMutable() {
		return this.isMutable;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushDict(this);
	}

	private boolean isDict(Ty ty) {
		ty = ty.base();
		if (ty instanceof GenericTy) {
			Ty base = ((GenericTy) ty).getBaseType().base();
			return (base == Ty.tDict || base == Ty.tMDict);
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
			this.setType(this.isMutable() ? Ty.tGeneric("Dict'", firstType) : Ty.tGeneric("Dict", firstType));
		}
		if (this.isDict(ret)) {
			Ty ty = ret.getParamType();
			if (!this.getType().getParamType().match(bEQ, ty, TypeMatcher.Update)) {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, ty);
				}
			}
			this.setType(ty);
		}
		return this.castType(env, ret);
	}

	private Ty guessInnerType(Ty t) {
		if (this.isDict(t)) {
			return t.getParamType();
		}
		return Ty.tUntyped();
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, this.isMutable() ? Ty.Mut + "dict" : "dict", 0, this.names.length, (n) -> {
			OStrings.appendQuoted(sb, this.names[n]);
			sb.append(":");
			OStrings.append(sb, this.args[n]);
		});
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Token(this.isMutable() ? Ty.Mut + "{" : "{");
		for (int i = 0; i < this.names.length; i++) {
			if (i > 0) {
				sh.Token(",");
			}
			sh.StringLiteral(this.names[i]);
			sh.Token(":");
			sh.Expr(this.args[i]);
		}
		sh.Token("}");
	}

}
