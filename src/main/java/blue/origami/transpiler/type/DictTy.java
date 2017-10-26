package blue.origami.transpiler.type;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DictCode;

public class DictTy extends MonadTy {

	public DictTy(String name, boolean isMutable, Ty innerType) {
		super(name, isMutable, innerType);
	}

	@Override
	public Ty newType(String name, Ty ty) {
		return new ListTy(name, this.isMutable(), ty);
	}

	@Override
	public Code getDefaultValue() {
		return new DictCode(this);
	}

}