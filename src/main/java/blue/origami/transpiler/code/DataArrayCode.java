package blue.origami.transpiler.code;

import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class DataArrayCode extends DataCode {

	public DataArrayCode(boolean isMutable, Code... values) {
		super(isMutable, TArrays.emptyNames, values);
	}

	public DataArrayCode(DataTy dt) {
		super(dt);
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		if (this.isUntyped() || (t.isArray() && !t.eq(this.getType()))) {
			Ty firstType = t.asArrayInner();
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
				Ty tt = this.args[i].getType();
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
			this.setType(this.isMutable() ? Ty.tArray(firstType) : Ty.tImArray(firstType));
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