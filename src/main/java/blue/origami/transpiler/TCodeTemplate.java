package blue.origami.transpiler;

import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.rule.NameExpr.TNameRef;

public class TCodeTemplate extends Template implements TNameRef {

	protected final String template;

	public TCodeTemplate(String name, TType returnType, TType[] paramTypes, String template) {
		super(name, returnType, paramTypes);
		this.template = template;
	}

	public TCodeTemplate(String template) {
		this(template, TType.tUntyped, EmptyConstants.emptyTypes, template);
	}

	@Override
	public String getDefined() {
		return this.template;
	}

	@Override
	public String format(Object... args) {
		return String.format(this.template, args);
	}

	@Override
	public TInst[] getInsts() {
		return EmptyConstants.emptyInsts;
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