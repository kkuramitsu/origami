package blue.origami.transpiler.code;

import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class DataDictCode extends DataCode {

	public DataDictCode(boolean isMutable, String[] names, Code[] values) {
		super(isMutable, names, values);
	}

	public DataDictCode(DataTy dt) {
		super(dt);
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		if (this.isUntyped() || (t.isDict() && t.equals(this.getType()))) {
			Ty firstType = t.asDictInner();
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, firstType);
				Ty tt = this.args[i].getType();
				if (tt.isUntyped()) {
					return this.StillUntyped();
				}
				if (firstType.isUntyped()) {
					firstType = tt;
				}
			}
			if (firstType.isUntyped()) {
				return this.StillUntyped();
			}
			this.setType(this.isMutable() ? Ty.tDict(firstType) : Ty.tImDict(firstType));
		}
		return super.asType(env, t);
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.args[0]);
		sb.append(this.isMutable() ? "{" : "[");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			StringCombinator.appendQuoted(sb, this.names[i]);
			sb.append(":");
			StringCombinator.append(sb, this.args[i]);
		}
		sb.append(this.isMutable() ? "}" : "]");
	}

}