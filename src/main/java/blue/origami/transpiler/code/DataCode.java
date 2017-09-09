package blue.origami.transpiler.code;

import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.OStrings;

public class DataCode extends CodeN {
	protected String[] names;
	boolean isMutable = false;

	public DataCode(boolean isMutable, String[] names, Code[] values) {
		super(values);
		this.names = names;
		this.isMutable = isMutable;
	}

	public DataCode(Ty dt) { // DefaultValue
		super(dt, TArrays.emptyCodes);
		this.names = TArrays.emptyNames;
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
	public Code asType(TEnv env, Ty ret) {
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
	public void emitCode(TEnv env, TCodeSection sec) {
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

}
