package blue.origami.transpiler.code;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TDataType;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class TDataArrayCode extends TDataCode {

	public TDataArrayCode(TCode... values) {
		super(TArrays.emptyNames, values);
	}

	public TDataArrayCode(TDataType dt) {
		super(dt);
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		if (this.isUntyped() || (t.isArrayType() && t.equals(this.getType()))) {
			TType firstType = t.asArrayInnerType();
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
				TType tt = this.args[i].getType();
				if (tt.isUntyped()) {
					return this;
				}
				if (firstType.isUntyped()) {
					firstType = tt;
				}
			}
			if (firstType.isUntyped()) {
				return this;
			}
			this.setType(TType.tArray(firstType));
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
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.args[0]);
		sb.append(this.isMutable() ? "{" : "[");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			StringCombinator.append(sb, this.args[i]);
		}
		sb.append(this.isMutable() ? "}" : "]");
	}

}