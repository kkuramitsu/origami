package origami.code;

import origami.type.OType;

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
