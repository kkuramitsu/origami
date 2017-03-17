package origami.code;

import origami.ODebug;
import origami.OEnv;
import origami.asm.OAsm;
import origami.type.OType;

public interface CommonCode extends OCode {

	@Override
	public default int getMatchCost() {
		return 0;
	}

	@Override
	public default boolean isUntyped() {
		return this.getType().isUntyped();
	}

	@Override
	public default OCode retypeLocal() {
		return this;
	}

	@Override
	public default OType valueType() {
		return this.getType().valueType();
	}

	@Override
	public default boolean isDefined() {
		return false;
	}

	@Override
	public default boolean hasReturnCode() {
		return false;
	}

	@Override
	public default OCode thisCode() {
		return this;
	}

	@Override
	public default OCode refineType(OEnv env, OType req) {
		return thisCode();
	}

	@Override
	public default void generate(OAsm gen) {
		ODebug.NotAvailable(this);
	}

	@Override
	public default Object eval(OEnv env) throws Throwable {
		ODebug.NotAvailable(this);
		return this;
	}

}