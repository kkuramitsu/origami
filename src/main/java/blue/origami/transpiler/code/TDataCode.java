package blue.origami.transpiler.code;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TDataType;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TNameHint;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.ODebug;

public class TDataCode extends TypedCodeN {
	private String[] names;

	public TDataCode(String[] names, TCode[] values) {
		super(TType.tUntyped, null, values);
		this.names = names;
	}

	public TDataCode(TDataType dt) {
		super(dt, null, TArrays.emptyCodes);
		this.names = TArrays.emptyNames;
	}

	public String[] getNames() {
		return this.names;
	}

	public boolean isArray() {
		return this instanceof TDataArrayCode;
	}

	public boolean isRange() {
		return this instanceof TDataRangeCode;
	}

	public boolean isDict() {
		return this instanceof TDataDictCode;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		if (this.isUntyped()) {
			TDataType dt = TType.tData(this.names).asLocal();
			for (int i = 0; i < this.args.length; i++) {
				String key = this.names[i];
				TCode value = this.args[i];
				TNameHint hint = env.findNameHint(env, key);
				if (hint != null) {
					value = value.asType(env, hint.getType());
				} else {
					value = value.asType(env, TType.tUntyped);
					TType ty = value.guessType();
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
	public Template getTemplate(TEnv env) {
		return env.getTemplate("{}");
	}

	@Override
	public String strOut(TEnv env) {
		ODebug.TODO(this);
		return this.getTemplate(env).format();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushData(env, this);
	}

}
