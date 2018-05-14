package origami.tcode;

import origami.nez2.OStrings;

public abstract class TType implements OStrings {
	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

}

class TClassType extends TType {
	Class<?> c;

	TClassType(Class<?> c) {
		this.c = c;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.c.getSimpleName());
	}

}
