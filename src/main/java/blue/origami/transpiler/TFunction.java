package blue.origami.transpiler;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;

public class TFunction extends Template implements NameInfo {
	protected boolean isPublic = false;
	protected String[] paramNames;
	protected Tree<?> body;
	private VarDomain dom;

	public TFunction(boolean isPublic, String name, VarDomain dom, Ty returnType, String[] paramNames, Ty[] paramTypes,
			Tree<?> body) {
		super(name, returnType, paramTypes);
		this.isPublic = isPublic;
		this.dom = dom;
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
		return null;
	}

	public boolean isPublic() {
		return this.isPublic;
	}

	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public String format(Object... args) {
		return null;
		// return String.format(this.template, args);
	}

	@Override
	public Template update(TEnv env, Code[] params) {
		// Ty[] p = this.getParamTypes();
		// if (Ty.hasUntyped(p)) {
		// p = p.clone();
		// for (int i = 0; i < p.length; i++) {
		// if (p[i].isUntyped()) {
		// p[i] = params[i].getType();
		// }
		// if (p[i].isUntyped()) {
		// return null;
		// }
		// }
		// String sig = getSignature(this.name, p);
		// Template tp = env.get(sig, Template.class);
		// if (tp == null) {
		// Transpiler tr = env.getTranspiler();
		// tp = tr.defineFunction(this.isPublic, this.name, this.dom,
		// this.paramNames, p, this.getReturnType(),
		// this.body);
		// env.add(sig, tp);
		// }
		// return tp;
		// } else {
		return this.generate(env);
		// }
	}

	public Template generate(TEnv env) {
		Transpiler tr = env.getTranspiler();
		Template tp = tr.defineFunction(this.isPublic, this.name, this.dom, this.paramNames, this.paramTypes,
				this.returnType, this.body);
		this.setExpired();
		return tp;
	}

	static String getSignature(String name, Ty[] p) {
		StringBuilder sb = new StringBuilder();
		sb.append("#");
		sb.append(name);
		for (Ty t : p) {
			sb.append(":");
			sb.append(t);
		}
		return sb.toString();
	}

	@Override
	public boolean isNameInfo(TEnv env) {
		return !this.isExpired();
	}

	@Override
	public Code newCode(Tree<?> s) {
		return new FuncRefCode(this.name, this).setSource(s);
	}

}