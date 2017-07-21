package blue.origami.transpiler;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TNameCode.TFuncRefCode;
import blue.origami.transpiler.code.TParamCode;
import blue.origami.transpiler.rule.NameExpr.TNameRef;

public class TFunction extends TSkeleton implements TNameRef {
	protected boolean isPublic = false;
	protected String[] paramNames;
	protected Tree<?> body;

	public TFunction(String name, TType returnType, String[] paramNames, TType[] paramTypes, Tree<?> body) {
		super(name, returnType, paramTypes);
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
	public TParamCode newParamCode(TEnv env, String name, TCode[] params) {
		TType[] p = this.getParamTypes();
		if (TType.hasUntyped(p)) {
			p = p.clone();
			for (int i = 0; i < p.length; i++) {
				if (p[i].isUntyped()) {
					p[i] = params[i].getType();
				}
			}
			String sig = getSignature(name, p);
			TSkeleton tp = env.get(sig, TSkeleton.class);
			if (tp == null) {
				Transpiler tr = env.getTranspiler();
				tp = tr.defineFunction(this.isPublic, name, this.paramNames, p, this.getReturnType(), this.body);
				env.add(sig, tp);
			}
			return new TParamCode(tp, params);
		} else {
			Transpiler tr = env.getTranspiler();
			TSkeleton tp = tr.defineFunction(this.isPublic, name, this.paramNames, this.paramTypes, this.returnType,
					this.body);
			this.setDisabled();
			return new TParamCode(tp, params);
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
		return true;
	}

	@Override
	public TCode nameCode(TEnv env, String name) {
		// if (!TType.hasUntyped(this.getParamTypes())) {
		// Transpiler tr = env.getTranspiler();
		// TTemplate tp = tr.defineFunction(name, this.paramNames,
		// this.paramTypes, this.returnType, this.body);
		// this.setDisabled();
		// return new TFuncRefCode(name, tp);
		// }
		return new TFuncRefCode(name, this);
	}

}