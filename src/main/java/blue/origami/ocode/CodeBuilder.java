package blue.origami.ocode;

import blue.origami.lang.OEnv;

public interface CodeBuilder {
	public OEnv env();

	public default OCode value(Object v) {
		return env().v(v);
	}

	public default OCode and(OCode expr, OCode expr2) {
		return new AndCode(env(), expr, expr2);
	}

	public default OCode or(OCode expr, OCode expr2) {
		return new OrCode(env(), expr, expr2);
	}

}
