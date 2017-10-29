package blue.origami.transpiler.type;

import java.util.Set;
import java.util.TreeSet;
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

	DataTy() {
		this.isMutable = true;
		this.fields = new TreeSet<>();
	}

	DataTy(boolean isMutable, String... names) {
		this();
		this.isMutable = isMutable;
		for (String n : names) {
			this.fields.add(n);
		}
	}

	public String[] names() {
		if (this.fields == null) {
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

	public final boolean hasField(String field) {
		return this.hasField(field, VarLogger.Update);
	}

	public boolean hasField(String field, VarLogger logs) {
		return this.fields.contains(field);
	}

	public Ty fieldTy(Env env, AST s, String name) {
		if (this.hasField(name)) {
			NameHint hint = env.findGlobalNameHint(env, name);
			if (hint != null) {
				Ty ty = hint.getType();
				return ty == Ty.tThis ? this : ty;
			}
			throw new ErrorCode(s, TFmt.undefined_name__YY1, name);
		}
		throw new ErrorCode(s, TFmt.undefined_name__YY1_in_YY2, name, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(DataTy.this.isMutable ? "{" : "[");
		OStrings.joins(sb, this.names(), ",");
		sb.append(DataTy.this.isMutable ? "}" : "]");
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return false;
	}

	@Override
	public Ty memoed() {
		return this;
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return this;
	}

	public final boolean hasFields(Set<String> fields, VarLogger logs) {
		for (String f : fields) {
			if (!this.hasField(f, logs)) {
				return false;
			}
		}
		return true;
	}

	// f(b)
	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isVar()) {
			// VarTy varTy = (VarTy) codeTy.real();
			// if (varTy.isParameter()) {
			DataTy pt = new FlowDataTy();
			pt.hasFields(this.fields, logs);
			return (codeTy.acceptTy(false, pt, logs));
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
}
