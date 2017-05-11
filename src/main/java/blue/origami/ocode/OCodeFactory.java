package blue.origami.ocode;

import blue.origami.lang.OEnv;

public interface OCodeFactory {
	public OEnv env();

	public default OCode newValueCode(Object v) {
		return env().v(v);
	}

	public default OCode newAndCode(OCode expr, OCode expr2) {
		return new AndCode(env(), expr, expr2);
	}

	public default OCode newOrCode(OCode expr, OCode expr2) {
		return new OrCode(env(), expr, expr2);
	}

}
