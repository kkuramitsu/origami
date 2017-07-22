package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.TType;
import blue.origami.util.ODebug;

public class TNameCode extends EmptyTypedCode {
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
	public Template getTemplate(TEnv env) {
		return env.getTemplate("varname", "name", "%s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.lname);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushName(env, this);
	}

	// @Override
	// public OCode newAssignCode(OEnv env, OCode right) {
	// if (this.readOnly) {
	// throw new ErrorCode(env, OFmt.read_only__YY0, this.getName());
	// }
	// OType ty = this.getType();
	// return new AssignCode(ty, false, this.getName(), right.asType(env, ty));
	// }

	public final static class TFuncRefCode extends EmptyTypedCode {
		String name;
		Template template;

		public TFuncRefCode(String name, Template tp) {
			super(TType.tFunc(tp.getReturnType(), tp.getParamTypes()));
			this.name = name;
			this.template = tp;
		}

		@Override
		public Template getTemplate(TEnv env) {
			return env.getTemplate("funcref", "%s");
		}

		@Override
		public String strOut(TEnv env) {
			return this.getTemplate(env).format(this.template.getName());
		}

		@Override
		public void emitCode(TEnv env, TCodeSection sec) {
			ODebug.TODO(this);
		}

		@Override
		public TCode applyCode(TEnv env, TCode... params) {
			return env.findParamCode(env, this.name, params);
		}

	}

}
