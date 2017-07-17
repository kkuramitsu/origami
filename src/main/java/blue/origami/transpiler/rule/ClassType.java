package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OFmt;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TTypeCode;

public class ClassType implements TTypeRule {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TType type = this.parseType(env, t);
		if (type == null) {
			throw new TErrorCode(t, OFmt.undefined_type__YY0, t.getString());
		}
		return new TTypeCode(type);
	}

	public TType parseType(TEnv env, Tree<?> t) {
		String name = t.getString();
		// if (name.indexOf('.') > 0) {
		// try {
		// return env.t(Class.forName(name));
		// } catch (ClassNotFoundException e) {
		// throw new TypeNotFoundException(env, t,
		// OFmt.unfound_class__YY0_by_YY1, t.getString(), e);
		// }
		// }
		// return OTypeName.getType(env, name);
		return env.getType(name);
	}

}
