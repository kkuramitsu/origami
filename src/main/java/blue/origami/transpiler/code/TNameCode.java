package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.rule.NameExpr.TNameRef;
import blue.origami.util.ODebug;

public class TNameCode extends TypedCode0 {
	private Tree<?> nameTree = null;
	private final String lname;
	private final int refLevel;

	public TNameCode(Tree<?> nameTree) {
		this(nameTree.getString(), TType.tUntyped, 0);
		this.nameTree = nameTree;
	}

	public TNameCode(String name, TType ty, int refLevel) {
		super(ty);
		this.lname = name;
		this.refLevel = refLevel;
	}

	public String getName() {
		return this.lname;
	}

	public int getRefLevel() {
		return this.refLevel;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		if (this.getType().isUntyped()) {
			TNameRef ref = env.get(this.lname, TNameRef.class, (e, c) -> e.isNameRef(env) ? e : null);
			if (ref == null) {
				throw new TErrorCode(this.nameTree, TFmt.undefined_name__YY0, this.lname);
			}
			return ref.nameCode(env, this.lname).asType(env, t);
		}
		return super.asType(env, t);
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

	public final static class TFuncRefCode extends TypedCode0 {
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
			return new TExprCode(this.name, params);
		}

	}

}
