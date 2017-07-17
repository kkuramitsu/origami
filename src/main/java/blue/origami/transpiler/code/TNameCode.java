package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TNameCode extends TTypedCode {
	private final String lname;
	// private final boolean readOnly;

	public TNameCode(String name, TType ty) {
		super(ty);
		this.lname = name;
		// this.readOnly = true;
	}

	public String getName() {
		return this.lname;
	}

	@Override
	public TTemplate getTemplate(TEnv env) {
		return env.getTemplate("varname", "name", "%s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.lname);
	}

	// @Override
	// public OCode newAssignCode(OEnv env, OCode right) {
	// if (this.readOnly) {
	// throw new ErrorCode(env, OFmt.read_only__YY0, this.getName());
	// }
	// OType ty = this.getType();
	// return new AssignCode(ty, false, this.getName(), right.asType(env, ty));
	// }

	public final static class TFuncRefCode extends TTypedCode {
		String name;
		TTemplate template;

		public TFuncRefCode(String name, TTemplate tp) {
			super(TType.tFunc(tp.getReturnType(), tp.getParamTypes()));
			this.name = name;
			this.template = tp;
		}

		@Override
		public TTemplate getTemplate(TEnv env) {
			return env.getTemplate("funcref", "%s");
		}

		@Override
		public String strOut(TEnv env) {
			return this.getTemplate(env).format(this.template.getName());
		}

		@Override
		public TCode applyCode(TEnv env, TCode... params) {
			return env.findParamCode(env, this.name, params);
		}

	}

}
