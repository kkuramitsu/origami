package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.GenericTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMatchContext;
import origami.libnez.OStrings;

public class DictCode extends CodeN {
	protected String[] names;

	public DictCode(Ty dt) {
		super(dt, OArrays.emptyCodes);
		this.names = OArrays.emptyNames;
		assert (this.isDict(dt));
	}

	public DictCode(String[] names, Code[] values) {
		super(values);
		this.names = names;
		assert (names.length == values.length);
	}

	public String[] getNames() {
		return this.names;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushDict(this);
	}

	private boolean isDict(Ty ty) {
		ty = ty.devar().toImmutable();
		if (ty instanceof GenericTy) {
			Ty base = ((GenericTy) ty).getBaseType().devar();
			return (base == Ty.tDict);
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
			this.setType(Ty.tGeneric(Ty.tDict, firstType).toMutable());
		}
		if (this.isDict(ret)) {
			Ty ty = ret.getParamType();
			if (!this.getType().getParamType().match(TypeMatchContext.Update, bEQ, ty)) {
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
		return Ty.tVar(null);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "dict", 0, this.names.length, (n) -> {
			OStrings.appendQuoted(sb, this.names[n]);
			sb.append(":");
			OStrings.append(sb, this.args[n]);
		});
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Token("{");
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