package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.DataVarTy;
import blue.origami.transpiler.type.Ty;
import origami.nez2.OStrings;
import origami.nez2.Token;

public class DataCode extends CodeN {
	protected Token[] names;

	public DataCode(Token[] names, Code[] values) {
		super(values);
		this.names = names;
	}

	public DataCode(Ty dt) { // DefaultValue
		super(dt, OArrays.emptyCodes);
		this.names = OArrays.emptyTokens;
	}

	public String[] getNames() {
		return NameHint.names(this.names);
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			if (this.args.length == 0) {
				this.setType(new DataVarTy().toMutable());
			} else {
				DataTy dt = Ty.tData(NameHint.names(this.names));
				for (int i = 0; i < this.args.length; i++) {
					Token key = this.names[i];
					Code value = this.args[i];
					String name = key.getSymbol();
					Ty ty = env.findNameHint(name);
					if (ty != null) {
						value = value.asType(env, ty);
					} else {
						ty = Ty.tVar(null);
						value = value.asType(env, ty);
						if (ty == value.getType()) {
							throw new ErrorCode(key, TFmt.no_type_hint__YY1, name);
						}
						ODebug.trace("implicit name definition %s as %s", name, ty);
						NameHint.addNameHint(env.getTranspiler(), key, ty);
					}
					this.args[i] = value;
				}
				this.setType(dt.toMutable());
			}
		}
		return super.castType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushData(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "data", 0, this.names.length, (n) -> {
			sb.append(this.names[n]);
			sb.append(":");
			OStrings.append(sb, this.args[n]);
		});
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Token("{");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sh.Token(",");
			}
			sh.Name(this.names[i].getSymbol());
			sh.Token(":");
			sh.Expr(this.args[i]);
		}
		sh.Token("}");
	}

}
