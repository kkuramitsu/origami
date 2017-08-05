package blue.origami.transpiler;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TFuncRefCode;
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
	public boolean isExpired() {
		return this.paramNames == null;
	}

	void setExpired() {
		this.paramNames = null;
		this.body = null;
	}

	@Override
	public String getDefined() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isPublic() {
		return this.isPublic;
	}

	public String[] getParamNames() {
		return this.paramNames;
	}

	public TCode getCode(TEnv env) {
		return env.parseCode(env, this.body).asType(env, this.returnType);
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
			return this.generate(env);
		}
	}

	public Template generate(TEnv env) {
		Transpiler tr = env.getTranspiler();
		Template tp = tr.defineFunction(this.isPublic, this.name, this.paramNames, this.paramTypes, this.returnType,
				this.body);
		this.setExpired();
		// env.add(this.name, tp); already added in defineFunction
		return tp;
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
		return this.isExpired();
	}

	@Override
	public TCode nameCode(TEnv env, String name) {
		if (!this.isExpired()) {
			Transpiler tr = env.getTranspiler();
			Template tp = tr.defineFunction(this.isPublic, this.name, this.paramNames, this.paramTypes, this.returnType,
					this.body);
			this.setExpired();
			return new TFuncRefCode(name, tp);
		}
		return new TFuncRefCode(name, this);
	}

}