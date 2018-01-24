package blue.origami.transpiler.type;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.ErrorCode;

public class DataTy extends Ty {

	TreeSet<String> fields;

	DataTy() {
		this.fields = new TreeSet<>();
	}

	DataTy(String... names) {
		this();
		for (String n : names) {
			this.fields.add(n);
		}
	}

	@Override
	public String keyOfArrows() {
		return "{}";
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("{");
		OStrings.joins(sb, this.fields(), ",");
		sb.append("}");
	}

	@Override
	public boolean eq(Ty ty) {
		Ty right = ty.devar();
		if (this == right) {
			return true;
		}
		if (right instanceof DataTy) {
			DataTy dt = (DataTy) right;
			if (dt.size() == this.size()) {
				for (String field : this.fields) {
					if (!dt.hasField2(field)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	// @Override
	// public Ty inferType(TypeMatchContext tmx) {
	// DataTy pt = new DataVarTy();
	// pt.hasFields(tmx, this.fields);
	// return pt;
	// }

	@Override
	public boolean matchBase(boolean sub, Ty right) {
		return right.isData() && this.eq(right) || (sub && right.hasSuperType(this));
	}

	@Override
	public boolean hasSuperType(Ty left0) {
		if (left0 instanceof DataTy) {
			DataTy left = (DataTy) left0;
			for (String f : left.fields) {
				if (!this.hasField2(f)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public int size() {
		if (this.fields == null) {
			return 0;
		}
		return this.fields.size();
	}

	public String[] fields() {
		if (this.fields == null) {
			return OArrays.emptyNames;
		}
		return this.fields.toArray(new String[this.fields.size()]);
	}

	@Override
	public Ty resolveFieldType(Env env, AST s, String name) {
		if (this.hasField2(name)) {
			Ty ty = env.findNameHint(name);
			if (ty != null) {
				return ty == Ty.tThis ? this : ty;
			}
			throw new ErrorCode(s, TFmt.no_type_hint__YY1, name);
		}
		return super.resolveFieldType(env, s, name);
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return f.test(this);
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		return f.apply(this);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this;
	}

	@Override
	public Ty memoed() {
		assert (this.isMemoed());
		return this;
	}

	public final boolean hasFields(TypeMatchContext tmx, Set<String> fields) {
		for (String f : this.fields) {
			if (!this.hasField(tmx, f)) {
				return false;
			}
		}
		return true;
	}

	public boolean hasField(TypeMatchContext tmx, String field) {
		return this.fields.contains(field);
	}

	public final boolean hasField2(String field) {
		return this.hasField(TypeMatchContext.Update, field);
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.forDataType(this);
	}

}
