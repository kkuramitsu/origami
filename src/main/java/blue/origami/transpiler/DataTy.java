package blue.origami.transpiler;

import java.util.Arrays;
import java.util.function.Predicate;

import blue.origami.konoha5.DSymbol;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.util.StringCombinator;

abstract class MonadTy extends Ty {
	boolean isImmutable;
	final Ty innerType;

	MonadTy(Ty innerType) {
		this.innerType = innerType;
	}

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

	// FIXME
	@Override
	public String strOut(TEnv env) {
		return null;
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

	public boolean isImmutable() {
		return this.isImmutable;
	}

	public DataTy asImmutable() {
		this.isImmutable = true;
		return this;
	}

	public Ty asParameter() {
		this.isParameter = true;
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

	boolean growing;
	DSymbol[] fields;

	DataTy() {
		this.growing = false;
		this.fields = new DSymbol[0];
	}

	DataTy(boolean growing, String... names) {
		this.growing = growing;
		this.fields = Arrays.stream(names).map(x -> DSymbol.unique(x)).toArray(DSymbol[]::new);
	}

	DataTy(boolean growing, DSymbol[] names) {
		this.growing = growing;
		this.fields = names;
	}

	@Override
	public Code getDefaultValue() {
		return new DataCode(this.isImmutable ? Ty.tImRecord() : Ty.tRecord());
	}

	public boolean hasField(DSymbol field) {
		for (DSymbol f : this.fields) {
			if (field == f) {
				return true;
			}
		}
		return false;
	}

	public void addField(DSymbol field) {
		if (this.growing && !this.hasField(field)) {
			DSymbol[] nf = new DSymbol[this.fields.length + 1];
			System.arraycopy(this.fields, 0, nf, 0, this.fields.length);
			nf[this.fields.length] = field;
			this.fields = nf;
		}
	}

	// @Override
	// public void checkField(Tree<?> at, String name, boolean ext) {
	// }

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

	//
	public void checkGetField(Tree<?> at, String name) {
		DSymbol f = DSymbol.unique(name);
		if (!this.hasField(f)) {
			if (this.growing || this.isParameter) {
				this.addField(f);
			} else {
				throw new ErrorCode(at, TFmt.undefined_name__YY0_in_YY1, name, this);
			}
		}
	}

	// public void sync(TDataType dt) {
	// if (this.isParameter) {
	// if (dt.isMutable) {
	// this.isMutable = true;
	// }
	// if (this.content == EmptyContent) {
	// this.content = dt.content.clone();
	// }
	// for (DSymbol f : dt.getFields()) {
	// this.addField(f);
	// }
	// }
	// }

	@Override
	public Ty getInnerTy() {
		return null;
	}

	/* TTypeApi */

	@Override
	public boolean is(Predicate<DataTy> f) {
		return f.test(this);
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty t, boolean updated) {
		if (t instanceof VarTy) {
			return (t.acceptTy(false, this, updated));
		}
		if (t instanceof DataTy) {
			DataTy dt = (DataTy) t;
			if (this.growing || dt.growing) {
				return true;
			}
			for (DSymbol f : this.fields) {
				if (!dt.hasField(f)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public String key() {
		return this.toString();
	}

	@Override
	public String strOut(TEnv env) {
		return null;
	}

}