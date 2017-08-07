package blue.origami.transpiler;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.TNameRef;

public class TCodeTemplate extends Template implements TNameRef {

	protected final String template;

	public TCodeTemplate(String name, Ty returnType, Ty[] paramTypes, String template) {
		super(name, returnType, paramTypes);
		this.template = template;
	}

	public TCodeTemplate(String template) {
		this(template, Ty.tUntyped, TArrays.emptyTypes, template);
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
		return TArrays.emptyInsts;
	}

	@Override
	public String toString() {
		return super.toString() + "=" + this.template;
	}

	@Override
	public boolean isNameRef(TEnv env) {
		return true;
	}

	@Override
	public Code nameCode(TEnv env, String name) {
		return new FuncRefCode(name, this);
	}

}