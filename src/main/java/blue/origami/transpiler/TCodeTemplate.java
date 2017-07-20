package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TParamCode;
import blue.origami.transpiler.rule.NameExpr.TNameRef;

public class TCodeTemplate extends TSkeleton implements TNameRef {

	protected String template = null;

	public TCodeTemplate(String name, TType returnType, TType[] paramTypes, String template) {
		super(name, returnType, paramTypes);
		this.template = template;
	}

	public TCodeTemplate(String template) {
		this(template, TType.tUntyped, TConsts.emptyTypes, template);
	}

	@Override
	public String format(Object... args) {
		return String.format(this.template, args);
	}

	@Override
	public TInst[] getInsts() {
		return TConsts.emptyInsts;
	}

	@Override
	public TParamCode newParamCode(TEnv env, String name, TCode[] params) {
		return new TParamCode(this, params);
	}

	@Override
	public String toString() {
		return super.toString() + "=" + this.template;
	}

	@Override
	public boolean isNameRef(TEnv env) {
		return !this.getReturnType().isUntyped();
	}

	@Override
	public TCode nameCode(TEnv env, String name) {
		return new TNameCode.TFuncRefCode(name, this);
	}

}