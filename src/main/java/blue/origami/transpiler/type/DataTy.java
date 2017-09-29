package blue.origami.transpiler.type;

import java.util.Set;
import java.util.TreeSet;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.util.OStrings;

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

	@Override
	public boolean isNonMemo() {
		return false;
	}

	public String[] names() {
		if (this.fields == null) {
			return TArrays.emptyNames;
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

	public Ty fieldTy(TEnv env, Tree<?> s, String name) {
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
	public boolean hasVar() {
		return false;
	}

	@Override
	public Ty finalTy() {
		return this;
	}

	// public void checkSetField(Tree<?> at, String name) {
	// if (!this.isMutable) {
	// throw new ErrorCode(TFmt.immutable_data);
	// } else {
	// this.checkGetField(at, name);
	// this.isMutable = true;
	// }
	// }
	//
	// private boolean isGrowing() {
	// return this.growing || this.isParameter;
	// }
	//
	// //
	// public void checkGetField(Tree<?> at, String name) {
	// DSymbol f = DSymbol.unique(name);
	// if (!this.hasField(f)) {
	// if (this.isGrowing()) {
	// this.addField(f);
	// } else {
	// throw new ErrorCode(at, TFmt.undefined_name__YY0_in_YY1, name, this);
	// }
	// }
	// }

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
			VarTy varTy = (VarTy) codeTy.real();
			if (varTy.isParameter()) {
				DataTy pt = new FlowDataTy();
				pt.hasFields(this.fields, logs);
				return (codeTy.acceptTy(false, pt, logs));
			}
			return (codeTy.acceptTy(false, this, logs));
		}
		if (codeTy.isData()) {
			DataTy dt = (DataTy) codeTy.real();
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
	public <C> C mapType(TypeMap<C> codeType) {
		return codeType.forDataType(this);
	}
}
