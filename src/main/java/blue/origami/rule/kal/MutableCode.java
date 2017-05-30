package blue.origami.rule.kal;

import blue.origami.lang.type.OType;
import blue.origami.ocode.OCode;
import blue.origami.ocode.OSourceCode;
import blue.origami.ocode.OWrapperCode;

public class MutableCode extends OSourceCode<OCode> implements OWrapperCode {

	public MutableCode(OCode handled) {
		super(handled, handled.getTypeSystem().newMutableType(handled.getType()));
	}

	@Override
	public OCode wrapped() {
		return this.getHandled();
	}

	@Override
	public void wrap(OCode code) {
		this.setHandled(code);
	}

	@Override
	public OType valueType() {
		return this.getType();
	}

}
