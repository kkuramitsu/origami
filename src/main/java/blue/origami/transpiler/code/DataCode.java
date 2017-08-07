package blue.origami.transpiler.code;

import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TNameHint;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class DataCode extends CodeN {
	protected String[] names;
	boolean isMutable = false;

	public DataCode(boolean isMutable, String[] names, Code[] values) {
		super(values);
		this.names = names;
		this.isMutable = isMutable;
	}

	public DataCode(DataTy dt) { // DefaultValue
		super(dt, TArrays.emptyCodes);
		this.names = TArrays.emptyNames;
		this.isMutable = !dt.isImmutable();
	}

	public String[] getNames() {
		return this.names;
	}

	public boolean isMutable() {
		return !this.isMutable;
	}

	public boolean isArray() {
		return this instanceof DataArrayCode;
	}

	public boolean isRange() {
		return this instanceof DataRangeCode;
	}

	public boolean isDict() {
		return this instanceof DataDictCode;
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		if (this.isUntyped()) {
			DataTy dt = Ty.tData(this.names).asLocal();
			for (int i = 0; i < this.args.length; i++) {
				String key = this.names[i];
				Code value = this.args[i];
				TNameHint hint = env.findNameHint(env, key);
				if (hint != null) {
					value = value.asType(env, hint.getType());
				} else {
					value = value.asType(env, Ty.tUntyped);
					Ty ty = value.guessType();
					// ODebug.trace("undefined symbol %s as %s", key, ty);
					env.addTypeHint(env, key, ty);
				}
				if (value.getType().isUntyped()) {
					return this;
				}
				this.args[i] = value;
			}
			this.setType(dt);
		}
		return super.asType(env, t);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushData(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.args[0]);
		sb.append(this.isMutable() ? "{" : "[");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(this.names[i]);
			sb.append(":");
			StringCombinator.append(sb, this.args[i]);
		}
		sb.append(this.isMutable() ? "}" : "]");
	}

}
