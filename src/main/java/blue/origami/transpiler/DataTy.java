package blue.origami.transpiler;

import java.util.Arrays;

import blue.origami.konoha5.DSymbol;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class DataTy extends Ty implements StringCombinator {
	public final static boolean Mutable = false;
	public final static boolean Immutable = true;
	boolean isImmutable = false;
	boolean isMutable = false;

	// Scoping
	boolean isLocal = false;
	boolean isParameter = false;

	private static Content Variant = new Variant();

	// Content
	abstract static class Content {
		Ty getInnerTy() {
			return null;
		}

		public void checkArray(Tree<?> at, Ty t) {
			throw new ErrorCode(at, TFmt.unsupported_operator);
		}

		public void checkDict(Tree<?> at, Ty t) {
			throw new ErrorCode(at, TFmt.unsupported_operator);
		}

		public void checkField(Tree<?> at, String name, boolean ext) {
			throw new ErrorCode(at, TFmt.unsupported_operator);
		}

		public Code getDefaultValue() {
			return null;
		}

		public abstract void strOut(DataTy self, StringBuilder sb);

		public abstract boolean hasVar();

		public abstract boolean acceptType(DataTy self, boolean sub, DataTy dt, boolean updated);

		public abstract boolean isDynamic();

		public abstract Ty nomTy(DataTy self);

		public abstract Content dup(VarDomain dom);
	}

	static class Variant extends Content {
		@Override
		public void strOut(DataTy self, StringBuilder sb) {
			sb.append("?");
		}

		@Override
		public boolean hasVar() {
			return true;
		}

		@Override
		public Content dup(VarDomain dom) {
			return this;
		}

		@Override
		public boolean acceptType(DataTy self, boolean sub, DataTy dt, boolean updated) {
			return false;
		}

		@Override
		public boolean isDynamic() {
			return true;
		}

		@Override
		public Ty nomTy(DataTy ty) {
			return ty;
		}
	}

	abstract class InnerData extends Content {
		final Ty innerType;

		InnerData(Ty innerType) {
			this.innerType = innerType;
		}

		@Override
		Ty getInnerTy() {
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

	}

	class ArrayData extends InnerData {

		ArrayData(Ty innerType) {
			super(innerType);
		}

		@Override
		public void checkArray(Tree<?> at, Ty t) {
			// if (!t.equals(at)) {
			// throw new TErrorCode(at, TFmt.mixed_array_YY0_YY1,
			// this.innerType, t);
			// }
		}

		@Override
		public Code getDefaultValue() {
			return new DataCode(DataTy.this.isImmutable ? Ty.tImArray(this.innerType) : Ty.tArray(this.innerType));
		}

		@Override
		public void strOut(DataTy self, StringBuilder sb) {
			if (self.isImmutable) {
				sb.append("{");
			}
			StringCombinator.append(sb, this.innerType);
			sb.append("*");
			if (self.isImmutable) {
				sb.append("}");
			}
		}

		@Override
		public Content dup(VarDomain dom) {
			if (this.hasVar()) {
				return new ArrayData(this.innerType.dupTy(dom));
			}
			return this;
		}

		@Override
		public boolean acceptType(DataTy self, boolean sub, DataTy dt, boolean updated) {
			return dt.isArray() && this.innerType.acceptTy(false, dt.getInnerType(), updated);
		}

		@Override
		public Ty nomTy(DataTy self) {
			Ty ty = this.innerType.nomTy();
			if (ty != this.innerType) {
				return self.isImmutable ? Ty.tImArray(ty) : Ty.tArray(ty);
			}
			return self;
		}

	}

	class DictData extends InnerData {
		DictData(Ty innerType) {
			super(innerType);
		}

		@Override
		public void checkDict(Tree<?> at, Ty t) {

		}

		@Override
		public void strOut(DataTy self, StringBuilder sb) {
			if (!self.isImmutable) {
				sb.append("$");
			}
			sb.append("Dict[");
			StringCombinator.append(sb, this.innerType);
			sb.append("]");
		}

		@Override
		public Content dup(VarDomain dom) {
			if (this.hasVar()) {
				return new DictData(this.innerType.dupTy(dom));
			}
			return this;
		}

		@Override
		public boolean acceptType(DataTy self, boolean sub, DataTy dt, boolean updated) {
			return dt.isDict() && this.innerType.acceptTy(false, dt.getInnerType(), updated);
		}

		@Override
		public Ty nomTy(DataTy dt) {
			Ty ty = this.innerType.nomTy();
			if (ty != this.innerType) {
				return dt.isImmutable ? Ty.tImDict(ty) : Ty.tDict(ty);
			}
			return dt;
		}

	}

	class Record extends Content {
		boolean growing;
		DSymbol[] fields;

		Record(boolean growing, String... names) {
			this.growing = growing;
			this.fields = Arrays.stream(names).map(x -> DSymbol.unique(x)).toArray(DSymbol[]::new);
		}

		Record(boolean growing, DSymbol[] names) {
			this.growing = growing;
			this.fields = names;
		}

		// @Override
		// public boolean equals(Object o) {
		// if (o instanceof Record) {
		// Record record = (Record) o;
		// for (DSymbol f : record.fields) {
		// if (!this.hasField(f)) {
		// return false;
		// }
		// }
		// return true;
		// }
		// return false;
		// }

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

		@Override
		public void checkField(Tree<?> at, String name, boolean ext) {
			DSymbol f = DSymbol.unique(name);
			if (!this.hasField(f)) {
				if (ext && this.growing) {
					this.addField(f);
				} else {
					throw new ErrorCode(at, TFmt.undefined_name__YY0__YY1, name, this);
				}
			}
		}

		@Override
		public void strOut(DataTy self, StringBuilder sb) {
			sb.append(DataTy.this.isImmutable ? "[" : "{");
			int c = 0;
			for (DSymbol s : this.fields) {
				if (c > 0) {
					sb.append(",");
				}
				sb.append(s);
				c++;
			}
			sb.append(DataTy.this.isImmutable ? "]" : "}");
		}

		@Override
		public boolean hasVar() {
			return false;
		}

		@Override
		public Content dup(VarDomain dom) {
			if (this.hasVar()) {
				return new Record(this.growing, this.fields);
			}
			return this;
		}

		/* if sub == true self <: dt */
		@Override
		public boolean acceptType(DataTy self, boolean sub, DataTy dt, boolean updated) {
			if (dt.content instanceof Record) {
				Record r1 = (Record) self.content;
				Record r2 = (Record) dt.content;
				if (r1.growing || r2.growing) {
					return true;
				}
				for (DSymbol f : r1.fields) {
					if (!r2.hasField(f)) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean isDynamic() {
			return this.growing;
		}

		@Override
		public Ty nomTy(DataTy ty) {
			return ty;
		}

	}

	Content content;

	DataTy(Content content) {
		this.content = content;
		this.isImmutable = false;
		this.isParameter = false;
		this.isLocal = false;
	}

	DataTy() {
		this(Variant);
	}

	DataTy(boolean isDict, Ty innerType) {
		this(Variant);
		this.content = isDict ? new DictData(innerType.nomTy()) : new ArrayData(innerType.nomTy());
	}

	DataTy(boolean growing, String... names) {
		this(Variant);
		this.content = new Record(growing, names);
	}

	@Override
	public boolean isUntyped() {
		return this.content instanceof Variant;
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.content.strOut(this, sb);
	}

	public void checkSetIndex(Tree<?> at, Ty t) {
		if (this.isImmutable) {
			throw new ErrorCode(TFmt.immutable_data);
		} else {
			this.checkGetIndex(at, t);
			this.isMutable = true;
		}
	}

	public void checkGetIndex(Tree<?> at, Ty t) {
		if (!t.isUntyped()) {
			if (this.content == Variant) {
				this.content = new ArrayData(t);
			}
			this.content.checkArray(at, t);
		}
	}

	public void checkSetDict(Tree<?> at, Ty t) {
		if (this.isImmutable) {
			throw new ErrorCode(TFmt.immutable_data);
		} else {
			this.checkGetDict(at, t);
			this.isMutable = true;
		}
	}

	public void checkGetDict(Tree<?> at, Ty t) {
		if (!t.isUntyped()) {
			if (this.content == Variant) {
				this.content = new DictData(t);
			}
			this.content.checkDict(at, t);
		}
	}

	public void checkSetField(Tree<?> at, String name) {
		if (this.isImmutable) {
			throw new ErrorCode(TFmt.immutable_data);
		} else {
			if (this.content == Variant) {
				this.content = new Record(true, name);
			}
			this.content.checkField(at, name, this.isParameter);
			this.isMutable = true;
		}
	}

	public void checkGetField(Tree<?> at, String name) {
		if (this.content == Variant) {
			this.content = new Record(true, name);
		}
		this.content.checkField(at, name, this.isParameter);
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
	public Code getDefaultValue() {
		return this.content.getDefaultValue();
	}

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

	public Ty getInnerType() {
		return this.content.getInnerTy();
	}

	/* TTypeApi */

	@Override
	public boolean isArray() {
		return this.content instanceof ArrayData;
	}

	@Override
	public Ty asArrayInner() {
		if (this.isArray()) {
			return this.content.getInnerTy();
		}
		return super.asArrayInner();
	}

	@Override
	public boolean isDict() {
		return this.content instanceof ArrayData;
	}

	@Override
	public Ty asDictInner() {
		if (this.isDict()) {
			return this.content.getInnerTy();
		}
		return super.asDictInner();
	}

	@Override
	public boolean hasVar() {
		return this.content.hasVar();
	}

	@Override
	public Ty dupTy(VarDomain dom) {
		if (this.hasVar()) {
			return new DataTy(this.content.dup(dom));
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty t, boolean updated) {
		if (t instanceof VarTy) {
			return (t.acceptTy(false, this, updated));
		}
		if (t instanceof DataTy) {
			DataTy dt = (DataTy) t;
			if (this.content instanceof Variant) {
				ODebug.TODO("variant type");
			}
			return this.content.acceptType(this, sub, dt, updated);
		}
		return false;
	}

	@Override
	public Ty nomTy() {
		return this.content.nomTy(this);
	}

	@Override
	public boolean isDynamic() {
		return this.content.isDynamic();
	}

	@Override
	public String strOut(TEnv env) {
		return this.content.toString();
	}

	@Override
	public String key() {
		return this.toString();
	}

}