package blue.origami.transpiler.type;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataDictCode;

public class DictTy extends MonadTy {

	public DictTy(String name, Ty innerType) {
		super(name, innerType);
	}

	@Override
	public Code getDefaultValue() {
		return new DataDictCode(this);
	}

}