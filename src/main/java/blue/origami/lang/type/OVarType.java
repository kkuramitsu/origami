package blue.origami.lang.type;

import blue.origami.lang.OEnv;
import blue.origami.ocode.OCode;
import blue.origami.util.ODebug;

public class OVarType extends PhantomType {
	private String varName;
	// private OType[] upperTypes = OType.emptyTypes;
	// private OType[] lowerTypes = OType.emptyTypes;

	public OVarType(String varName, OType wrapped) {
		super(wrapped);
		this.varName = varName;
	}

	@Override
	public String getLocalName() {
		return varName + ": " + this.thisType().getLocalName();
	}

	@Override
	public boolean isUntyped() {
		return thisType() instanceof OUntypedType;
	}

	@Override
	public void setType(OType t) {
		if (isUntyped() && !t.isUntyped()) {
			ODebug.trace("infer %s as %s", varName, t);
			super.setType(t);
		}
	}

	@Override
	public OCode accept(OEnv env, OCode code, TypeChecker ext) {
		OType t = code.valueType();
		if (isUntyped()) {
			appendUpperBounds(t);
			return code;
		}
		// if (!realType.isNullable() && t.isNullable()) {
		// realType = this.getTypeSystem().newNullableType(realType);
		// }
		return thisType().accept(env, code, ext);
	}

	public void appendUpperBounds(OType t) {
		this.setType(t);
		// for (OType u : this.upperTypes) {
		// if (u.eq(t)) {
		// break;
		// }
		// }
		// append(this.upperTypes, t);
	}

	public void appendLowerBounds(OType t) {
		this.setType(t);
		// for (OType u : this.lowerTypes) {
		// if (u.eq(t)) {
		// break;
		// }
		// }
		// append(this.lowerTypes, t);
	}

}
