package blue.origami.transpiler;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TNameCode.TFuncRefCode;
import blue.origami.transpiler.rule.NameExpr.TNameRef;

public class TFunction extends Template implements TNameRef {
	protected boolean isPublic = false;
	protected String[] paramNames;
	protected Tree<?> body;

	public TFunction(boolean isPublic, String name, TType returnType, String[] paramNames, TType[] paramTypes,
			Tree<?> body) {
		super(name, returnType, paramTypes);
		this.isPublic = isPublic;
		this.paramNames = paramNames;
		this.body = body;
	}

	@Override
	public boolean isEnabled() {
		return this.paramNames != null;
	}

	void setDisabled() {
		this.paramNames = null;
		this.body = null;
	}

	@Override
	public String getDefined() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String format(Object... args) {
		return null;
		// return String.format(this.template, args);
	}

	@Override
	public Template update(TEnv env, TCode[] params) {
		TType[] p = this.getParamTypes();
		if (TType.hasUntyped(p)) {
			p = p.clone();
			for (int i = 0; i < p.length; i++) {
				if (p[i].isUntyped()) {
					p[i] = params[i].getType();
				}
				if (p[i].isUntyped()) {
					return null;
				}
			}
			String sig = getSignature(this.name, p);
			Template tp = env.get(sig, Template.class);
			if (tp == null) {
				Transpiler tr = env.getTranspiler();
				tp = tr.defineFunction(this.isPublic, this.name, this.paramNames, p, this.getReturnType(), this.body);
				env.add(sig, tp);
			}
			return tp;
		} else {
			Transpiler tr = env.getTranspiler();
			Template tp = tr.defineFunction(this.isPublic, this.name, this.paramNames, this.paramTypes, this.returnType,
					this.body);
			this.setDisabled();
			return tp;
		}
	}

	static String getSignature(String name, TType[] p) {
		StringBuilder sb = new StringBuilder();
		sb.append("#");
		sb.append(name);
		for (TType t : p) {
			sb.append(":");
			sb.append(t);
		}
		return sb.toString();
	}

	@Override
	public boolean isNameRef(TEnv env) {
		return this.isEnabled();
	}

	@Override
	public TCode nameCode(TEnv env, String name) {
		return new TFuncRefCode(name, this);
	}

}