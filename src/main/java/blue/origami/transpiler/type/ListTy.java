package blue.origami.transpiler.type;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataListCode;
import blue.origami.util.StringCombinator;

public class ListTy extends MonadTy {
	public static String ImmutableName = "List";
	public static String MutableName = "List'";

	public ListTy(String name, Ty innerType) {
		super(name, innerType);
		assert (innerType != null);
	}

	@Override
	public Code getDefaultValue() {
		return new DataListCode((ListTy) Ty.tMonad(this.name, this.innerTy));
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.innerTy);
		sb.append(this.isMutable() ? "[]" : "*");
	}

}