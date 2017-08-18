package blue.origami.transpiler;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import blue.origami.konoha5.DSymbol;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.util.StringCombinator;

abstract class MutableTy extends Ty {
	boolean isImmutable;
	final Ty innerType;

	MutableTy(Ty innerType) {
		this.innerType = innerType;
	}

	@Override
	public boolean isImmutable() {
		return this.isImmutable;
	}

	public boolean isMutable() {
		return !this.isImmutable;
	}

	@Override
	public Ty getInnerTy() {
		return this.innerType;
	}

	@Override
	public boolean isDynamic() {
		return this.innerType.isDynamic();
	}

	@Override
	public boolean hasVar() {
		return this.innerType.hasVar();
	}

	@Override
	public String key() {
		return this.toString();
	}
}

public class DataTy extends Ty {
	public final static boolean Mutable = false;
	public final static boolean Immutable = true;
	boolean isImmutable = false;
	boolean isMutable = false;

	// Scoping
	boolean isLocal = false;
	boolean isParameter = false;

	@Override
	public boolean isImmutable() {
		return this.isImmutable;
	}

	public DataTy asImmutable() {
		this.isImmutable = true;
		return this;
	}

	public DataTy asParameter() {
		this.isParameter = true;
		return this;
	}

	public DataTy asRecord() {
		this.growing = false;
		this.isParameter = false;
		return this;
	}

	public DataTy asLocal() {
		this.isLocal = true;
		return this;
	}

	public DataTy asNonLocal() {
		this.isLocal = false;
		return this;
	}

	boolean growing = false;
	DSymbol[] fields;

	DataTy() {
		this.fields = new DSymbol[0];
	}

	DataTy(String... names) {
		this.fields = Arrays.stream(names).map(x -> DSymbol.unique(x)).toArray(DSymbol[]::new);
	}

	// DataTy(boolean growing, DSymbol[] names) {
	// this.growing = growing;
	// this.fields = names;
	// }

	@Override
	public Code getDefaultValue() {
		return new DataCode(this.isImmutable ? Ty.tImRecord() : Ty.tRecord());
	}

	public int size() {
		return this.fields.length;
	}

	public boolean hasField(DSymbol field) {
		for (DSymbol f : this.fields) {
			if (field == f) {
				return true;
			}
		}
		return false;
	}

	public boolean hasFields(DSymbol... fields) {
		for (DSymbol f : fields) {
			if (!this.hasField(f)) {
				return false;
			}
		}
		return true;
	}

	private void addField(DSymbol field) {
		// ODebug.trace("DataType: %s + %s", this, field);
		DSymbol[] nf = new DSymbol[this.fields.length + 1];
		System.arraycopy(this.fields, 0, nf, 0, this.fields.length);
		nf[this.fields.length] = field;
		this.fields = nf;
	}

	private void addFields(DSymbol... fields) {
		for (DSymbol f : fields) {
			if (!this.hasField(f)) {
				this.addField(f);
			}
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(DataTy.this.isImmutable ? "[" : "{");
		StringCombinator.joins(sb, this.fields, ",");
		sb.append(DataTy.this.isImmutable ? "]" : "}");
	}

	@Override
	public boolean hasVar() {
		return false;
	}

	@Override
	public boolean isDynamic() {
		return this.growing;
	}

	@Override
	public Ty nomTy() {
		return this;
	}

	public void checkSetField(Tree<?> at, String name) {
		if (this.isImmutable) {
			throw new ErrorCode(TFmt.immutable_data);
		} else {
			this.checkGetField(at, name);
			this.isMutable = true;
		}
	}

	private boolean isGrowing() {
		return this.growing || this.isParameter;
	}

	//
	public void checkGetField(Tree<?> at, String name) {
		DSymbol f = DSymbol.unique(name);
		if (!this.hasField(f)) {
			if (this.isGrowing()) {
				this.addField(f);
			} else {
				throw new ErrorCode(at, TFmt.undefined_name__YY0_in_YY1, name, this);
			}
		}
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		return this;
	}

	// f(b)
	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, boolean updated) {
		if (codeTy instanceof VarTy) {
			if (((VarTy) codeTy).isParameter()) {
				DataTy pt = Ty.tData().asParameter();
				pt.addFields(this.fields);
				return (codeTy.acceptTy(false, pt, updated));
			}
			return (codeTy.acceptTy(false, this, updated));
		}
		if (codeTy instanceof DataTy) {
			DataTy dt = (DataTy) codeTy;
			// f(b) b: isParameter = true;
			if (dt.isParameter) {
				if (updated) {
					dt.addFields(this.fields);
				}
				return true;
			}
			if (dt.hasFields(this.fields)) {
				if (!sub) {
					return this.hasFields(dt.fields);
				}
				return true;
			}
			return false;
		}
		return false;
	}

	@Override
	public String key() {
		return "{}";
	}

	@Override
	public <C> C mapType(CodeType<C> codeType) {
		return codeType.mapType(this);
	}

	public Set<String> names() {
		TreeSet<String> nameSet = new TreeSet<>();
		for (DSymbol f : this.fields) {
			nameSet.add(f.toString());
		}
		return nameSet;
	}

}