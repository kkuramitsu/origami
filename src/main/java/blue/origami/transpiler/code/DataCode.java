package blue.origami.transpiler.code;

import blue.origami.transpiler.NameHint;
import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.Ty;

public class DataCode extends CodeN {
	protected String[] names;
	boolean isMutable = false;

	public DataCode(boolean isMutable, String[] names, Code[] values) {
		super(values);
		this.names = names;
		this.isMutable = isMutable;
	}

	public DataCode(Ty dt) { // DefaultValue
		super(dt, OArrays.emptyCodes);
		this.names = OArrays.emptyNames;
		this.isMutable = dt.isMutable();
	}

	public String[] getNames() {
		return this.names;
	}

	public boolean isMutable() {
		return this.isMutable;
	}

	public boolean isList() {
		return this instanceof DataListCode;
	}

	public boolean isRange() {
		return this instanceof DataRangeCode;
	}

	public boolean isDict() {
		return this instanceof DataDictCode;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			DataTy dt = Ty.tData(this.names);
			for (int i = 0; i < this.args.length; i++) {
				String key = this.names[i];
				Code value = this.args[i];
				NameHint hint = env.findGlobalNameHint(env, key);
				if (hint != null) {
					value = value.asType(env, hint.getType());
					if (!hint.equalsName(key) && hint.isLocalOnly()) {
						env.addGlobalName(env, key, hint.getType());
					} else {
						hint.useGlobal();
					}
				} else {
					Ty ty = Ty.tUntyped();
					value = value.asType(env, ty);
					if (ty == value.getType()) {
						throw new ErrorCode(value, TFmt.failed_type_inference);
					}
					ODebug.trace("implicit name definition %s as %s", key, ty);
					env.addGlobalName(env, key, ty);
				}
				this.args[i] = value;
			}
			this.setType(dt);
		}
		return super.castType(env, ret);
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		sec.pushData(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		// StringCombinator.append(sb, this.args[0]);
		sb.append(this.isMutable() ? "{" : "[");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			if (this.names.length > 0) {
				sb.append(this.names[i]);
				sb.append(":");
			}
			OStrings.append(sb, this.args[i]);
		}
		sb.append(this.isMutable() ? "}" : "]");
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Token(this.isMutable() ? "{" : "[");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sh.Token(",");
			}
			if (this.names.length > 0) {
				if (this instanceof DataDictCode) {
					sh.StringLiteral(this.names[i]);
				} else {
					sh.Name(this.names[i]);
				}
				sh.Token(":");
			}
			sh.Expr(this.args[i]);
		}
		sh.Token(this.isMutable() ? "}" : "]");
	}

}
