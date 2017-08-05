package blue.origami.transpiler;

import java.util.Arrays;

import blue.origami.konoha5.DSymbol;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDataCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.util.StringCombinator;

public class TDataType extends TType implements StringCombinator {
	public final static boolean Mutable = false;
	public final static boolean Immutable = true;
	boolean isImmutable = false;
	boolean isMutable = false;

	// Scoping
	boolean isLocal = false;
	boolean isParameter = false;

	private static Content EmptyContent = new EmptyContent();

	// Content
	static abstract class Content implements StringCombinator {
		TType getInnerType() {
			return null;
		}

		@Override
		public String toString() {
			return StringCombinator.stringfy(this);
		}

		public void checkArray(Tree<?> at, TType t) {
			throw new TErrorCode(at, TFmt.unsupported_operator);
		}

		public void checkDict(Tree<?> at, TType t) {
			throw new TErrorCode(at, TFmt.unsupported_operator);
		}

		public void checkField(Tree<?> at, String name, boolean ext) {
			throw new TErrorCode(at, TFmt.unsupported_operator);
		}

		public abstract boolean acceptType(TDataType self, TDataType dt);

		public TCode getDefaultValue() {
			return null;
		}

		public abstract boolean isVarType();

		public abstract Content dup(TVarDomain dom);
	}

	static class EmptyContent extends Content {
		@Override
		public void strOut(StringBuilder sb) {
			sb.append("Data");
		}

		@Override
		public boolean isVarType() {
			return true;
		}

		@Override
		public Content dup(TVarDomain dom) {
			return this;
		}

		@Override
		public boolean acceptType(TDataType self, TDataType dt) {
			return self == dt;
		}
	}

	static class ArrayContent extends Content {
		final TType innerType;

		ArrayContent(TType innerType) {
			this.innerType = innerType;
		}

		@Override
		TType getInnerType() {
			return this.innerType;
		}

		@Override
		public void checkArray(Tree<?> at, TType t) {
			// if (!t.equals(at)) {
			// throw new TErrorCode(at, TFmt.mixed_array_YY0_YY1,
			// this.innerType, t);
			// }
		}

		@Override
		public TCode getDefaultValue() {
			return new TDataCode(TType.tArray(this.innerType));
		}

		@Override
		public void strOut(StringBuilder sb) {
			StringCombinator.append(sb, this.innerType);
			sb.append("*");
		}

		@Override
		public String toString() {
			return StringCombinator.stringfy(this);
		}

		@Override
		public boolean isVarType() {
			return this.innerType.isVarType();
		}

		@Override
		public Content dup(TVarDomain dom) {
			if (this.isVarType()) {
				return new ArrayContent(this.innerType.dup(dom));
			}
			return this;
		}

		@Override
		public boolean acceptType(TDataType self, TDataType dt) {
			return dt.isArrayType() && self.getInnerType().acceptType(dt.getInnerType());
		}

	}

	static class DictContent extends ArrayContent {
		DictContent(TType innerType) {
			super(innerType);
		}

		@Override
		public void checkArray(Tree<?> at, TType t) {
			super.checkDict(at, t);
		}

		@Override
		public void checkDict(Tree<?> at, TType t) {
			super.checkDict(at, t);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("[");
			StringCombinator.append(sb, this.innerType);
			sb.append("]");
		}

		@Override
		public Content dup(TVarDomain dom) {
			if (this.isVarType()) {
				return new DictContent(this.innerType.dup(dom));
			}
			return this;
		}

		@Override
		public boolean acceptType(TDataType self, TDataType dt) {
			return dt.isDictType() && self.getInnerType().acceptType(dt.getInnerType());
		}

	}

	static class RecordContent extends Content {
		boolean growing;
		DSymbol[] fields;

		RecordContent(boolean growing, String... names) {
			this.growing = growing;
			this.fields = Arrays.stream(names).map(x -> DSymbol.unique(x)).toArray(DSymbol[]::new);
		}

		RecordContent(boolean growing, DSymbol[] names) {
			this.growing = growing;
			this.fields = names;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof RecordContent) {
				RecordContent record = (RecordContent) o;
				for (DSymbol f : record.fields) {
					if (!this.hasField(f)) {
						return false;
					}
				}
				return true;
			}
			return false;
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

		@Override
		public void checkField(Tree<?> at, String name, boolean ext) {
			DSymbol f = DSymbol.unique(name);
			if (!this.hasField(f)) {
				if (ext && this.growing) {
					this.addField(f);
				} else {
					throw new TErrorCode(at, TFmt.undefined_name__YY0__YY1, name, this);
				}
			}
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("{");
			int c = 0;
			for (DSymbol s : this.fields) {
				if (c > 0) {
					sb.append(",");
				}
				sb.append(s);
				c++;
			}
			sb.append("}");
		}

		@Override
		public boolean isVarType() {
			return this.growing;
		}

		@Override
		public Content dup(TVarDomain dom) {
			if (this.isVarType()) {
				return new RecordContent(this.growing, this.fields);
			}
			return this;
		}

		@Override
		public boolean acceptType(TDataType self, TDataType dt) {
			if (self.content instanceof RecordContent && dt.content instanceof RecordContent) {
				RecordContent r1 = (RecordContent) self.content;
				RecordContent r2 = (RecordContent) dt.content;
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

	}

	Content content;

	TDataType(Content content) {
		this.content = content;
		this.isImmutable = false;
		this.isParameter = false;
		this.isLocal = false;
	}

	TDataType() {
		this(EmptyContent);
	}

	TDataType(boolean isDict, TType innerType) {
		this(isDict ? new DictContent(innerType) : new ArrayContent(innerType));
	}

	TDataType(boolean growing, String... names) {
		this(new RecordContent(growing, names));
	}

	@Override
	public boolean isUntyped() {
		return this.content instanceof EmptyContent;
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.content.strOut(sb);
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

	public void checkSetIndex(Tree<?> at, TType t) {
		if (this.isImmutable) {
			throw new TErrorCode(TFmt.immutable_data);
		} else {
			this.checkGetIndex(at, t);
			this.isMutable = true;
		}
	}

	public void checkGetIndex(Tree<?> at, TType t) {
		if (!t.isUntyped()) {
			if (this.content == EmptyContent) {
				this.content = new ArrayContent(t);
			}
			this.content.checkArray(at, t);
		}
	}

	public void checkSetDict(Tree<?> at, TType t) {
		if (this.isImmutable) {
			throw new TErrorCode(TFmt.immutable_data);
		} else {
			this.checkGetDict(at, t);
			this.isMutable = true;
		}
	}

	public void checkGetDict(Tree<?> at, TType t) {
		if (!t.isUntyped()) {
			if (this.content == EmptyContent) {
				this.content = new DictContent(t);
			}
			this.content.checkDict(at, t);
		}
	}

	public void checkSetField(Tree<?> at, String name) {
		if (this.isImmutable) {
			throw new TErrorCode(TFmt.immutable_data);
		} else {
			if (this.content == EmptyContent) {
				this.content = new RecordContent(true, name);
			}
			this.content.checkField(at, name, this.isParameter);
			this.isMutable = true;
		}
	}

	public void checkGetField(Tree<?> at, String name) {
		if (this.content == EmptyContent) {
			this.content = new RecordContent(true, name);
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
	public TCode getDefaultValue() {
		return this.content.getDefaultValue();
	}

	public boolean isImmutable() {
		return this.isImmutable;
	}

	public TType asImmutable() {
		this.isImmutable = true;
		return this;
	}

	public TType asParameter() {
		this.isParameter = true;
		return this;
	}

	public TDataType asLocal() {
		this.isLocal = true;
		return this;
	}

	public TDataType asNonLocal() {
		this.isLocal = false;
		return this;
	}

	public TType getInnerType() {
		return this.content.getInnerType();
	}

	/* TTypeApi */

	@Override
	public boolean isArrayType() {
		return this.content instanceof ArrayContent;
	}

	@Override
	public TType asArrayInnerType() {
		if (this.isArrayType()) {
			return this.content.getInnerType();
		}
		return super.asArrayInnerType();
	}

	@Override
	public boolean isDictType() {
		return this.content instanceof ArrayContent;
	}

	@Override
	public TType asDictInnerType() {
		if (this.isDictType()) {
			return this.content.getInnerType();
		}
		return super.asDictInnerType();
	}

	@Override
	public boolean isVarType() {
		return this.content.isVarType();
	}

	@Override
	public TType dup(TVarDomain dom) {
		if (this.isVarType()) {
			return new TDataType(this.content.dup(dom));
		}
		return this;
	}

	@Override
	public boolean acceptType(TType t) {
		if (t instanceof TDataType) {
			TDataType dt = (TDataType) t;
			return this.content.acceptType(this, dt);
		}
		return false;
	}

	@Override
	public String strOut(TEnv env) {
		// TODO Auto-generated method stub
		return null;
	}

}