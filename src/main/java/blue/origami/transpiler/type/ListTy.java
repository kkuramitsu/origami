package blue.origami.transpiler.type;

import blue.origami.common.OStrings;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataListCode;

public class ListTy extends MonadTy {

	public ListTy(String name, boolean isMutable, Ty innerType) {
		super(name, isMutable, innerType);
		assert (innerType != null);
	}

	@Override
	public Ty newType(String name, Ty ty) {
		return new ListTy(name, this.isMutable(), ty);
	}

	@Override
	public Code getDefaultValue() {
		return new DataListCode((ListTy) Ty.tMonad(this.name, this.isMutable(), this.innerTy));
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.innerTy);
		sb.append(this.isMutable() ? "{}" : "[]");
	}

}