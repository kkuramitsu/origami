package blue.origami.transpiler.type;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.ErrorCode;

public class DataTy extends Ty {
	boolean isMutable = false;
	private String cnt = "";

	@Override
	public boolean isMutable() {
		return this.isMutable;
	}

	@Override
	public Ty toImmutable() {
		if (this.isMutable()) {
			return Ty.tRecord(this.names());
		}
		return this;
	}

	TreeSet<String> fields;

	public DataTy() {
		this.isMutable = true;
		this.fields = new TreeSet<>();
	}

	public DataTy(boolean isMutable) {
		this.isMutable = isMutable;
		this.fields = new TreeSet<>();
	}

	public DataTy(boolean isMutable, String... names) {
		this(isMutable);
		if (names.length != 0) {
			String first = names[0];
			int index = first.indexOf(' ');
			if (index > 0) {
				this.cnt = first.substring(index - 1);
			}
		}

		for (String n : names) {
			this.fields.add(n);
		}
	}

	public DataTy(boolean isMutable, int id, String... names) {
		this(isMutable);
		this.cnt = makeCnt(id);

		if (names.length != 0 && names[0].indexOf(' ') != -1) {
			for (String n : names) {
				this.fields.add(n);
			}
		}else{
			for (String n : names) {
				this.fields.add(n + this.cnt);
			}
		}
	}

	public String[] names() {
		if (this.fields == null || this.fields.size() == 0) {
			return OArrays.emptyNames;
		}
		return this.fields.toArray(new String[this.fields.size()]);
	}

	@Override
	public Code getDefaultValue() {
		return new DataCode(this.isMutable ? Ty.tData() : Ty.tRecord());
	}

	public int size() {
		if (this.fields == null) {
			return 0;
		}
		return this.fields.size();
	}

	public String getCnt() {
		return this.cnt;
	}

	public final boolean hasField(String field) {
		return this.hasField(field, TypeMatcher.Update);
	}

	public boolean hasField(String field, TypeMatcher logs) {
		if (field.indexOf(' ') == -1 && this.cnt.length() > 1) {
			return this.hasField(field + this.cnt, logs);
		}
		return this.fields.contains(field);
	}

	public Ty fieldTy(Env env, AST s, String name) {
		if (name.indexOf(' ') == -1 && this.cnt.length() > 1) {
			return this.fieldTy(env, s, name + this.cnt);
		}
		if (this.hasField(name)) {
			NameHint hint = env.findGlobalNameHint(env, name);
			if (hint != null) {
				Ty ty = hint.getType();
				return ty;
			}
			throw new ErrorCode(s, TFmt.undefined_name__YY1, name);
		}
		throw new ErrorCode(s, TFmt.undefined_name__YY1_in_YY2, name, this);
	}

	public static String makeCnt(int id) {
		return " " + String.valueOf(id) + "D";
	}

	public static String deleteCnt(String name) {
		int index = name.indexOf(' ');
		if (index != -1) {
			return name.substring(0, index);
		}
		return name;
	}

	public static String[] deleteCnts(String[] names) {
		String[] deletedNames = new String[names.length];
		for (int i = 0; i < names.length; i++) {
			deletedNames[i] = deleteCnt(names[i]);
		}
		return deletedNames;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(DataTy.this.isMutable ? "{" : "[");
		OStrings.joins(sb, deleteCnts(this.names()), ",");
		sb.append(DataTy.this.isMutable ? "}" : "]");
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
	public Ty memoed() {
		return this;
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this;
	}

	public final boolean hasFields(Set<String> fields, TypeMatcher logs) {
		for (String f : fields) {
			if (!this.hasField(f, logs)) {
				return false;
			}
		}
		return true;
	}

	// f(b)
	@Override
	public boolean match(boolean sub, Ty codeTy, TypeMatcher logs) {
		if (codeTy.isVar()) {
			// VarTy varTy = (VarTy) codeTy.real();
			// if (varTy.isParameter()) {
			DataTy pt = new FlowDataTy();
			pt.hasFields(this.fields, logs);
			return (codeTy.match(false, pt, logs));
			// }
			// return (codeTy.acceptTy(false, this, logs));
		}
		if (codeTy.isData()) {
			DataTy dt = (DataTy) codeTy.base();
			if (dt.hasFields(this.fields, logs)) {
				if (!sub) {
					return this.hasFields(dt.fields, logs);
				}
				return true;
			}
			return false;
		}
		return false;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.forDataType(this);
	}

	@Override
	public String keyFrom() {
		return "{}";
	}

}
