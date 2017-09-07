package blue.origami.transpiler;

import java.util.Arrays;
import java.util.HashMap;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.util.ODebug;

public class TFunction extends Template implements NameInfo {
	static int seq = 0;
	private int funcId;
	protected boolean isPublic = false;
	protected String[] paramNames;
	protected Tree<?> body;
	private Template generated = null;

	public TFunction(boolean isPublic, String name, Ty returnType, String[] paramNames, Ty[] paramTypes, Tree<?> body) {
		super(name, returnType, paramTypes);
		this.funcId = seq++;
		this.isPublic = isPublic;
		this.paramNames = paramNames;
		this.body = body;
	}

	public boolean isPublic() {
		return this.isPublic;
	}

	@Override
	public void used(TEnv env) {
		if (this.isUnused()) {
			super.used(env);
			VarDomain dom = new VarDomain(this.paramNames);
			Ty[] pats = dom.paramTypes(this.paramNames, this.paramTypes);
			Ty ret = dom.retType(this.returnType);
			this.paramTypes = pats;
			this.returnType = ret;
			Code code = env.typeBody(this.name, this.paramNames, pats, ret, dom, this.body);
			boolean isAbstract = code.hasSome(c -> c.isAbstract());
			dom.rename();
			this.paramTypes = Arrays.stream(pats).map(t -> t.finalTy()).toArray(Ty[]::new);
			this.returnType = ret.finalTy();
			// ODebug.trace(":::: abstract=%s %s ", isAbstract,
			// this.getFuncType());
			// System.out.println(":::::::" + this.name + "," + isAbstract + "
			// ::: " + this.getFuncType());
			if (!isAbstract && this.generated == null) {
				Transpiler tr = env.getTranspiler();
				this.generated = tr.defineFunction(this.isPublic, this.name, this.funcId, this.paramNames,
						this.paramTypes, this.returnType, code);
				ODebug.trace("generating %s", this.generated);
				this.setExpired();
			}
		}
	}

	@Override
	public boolean isAbstract() {
		return this.generated == null;
	}

	@Override
	public boolean isExpired() {
		return this.generated != null;
	}

	void setExpired() {
		this.paramNames = null;
		this.body = null;
	}

	@Override
	public String getDefined() {
		return this.generated == null ? "" : this.generated.getDefined();
	}

	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public String format(Object... args) {
		return null;
	}

	// let add(a, b) =
	// a[0] + b[0]

	// let reverse(a) =
	// dup = {}
	// [0 to < |a|].forEach(\i dup.push(a[|a|-1-i]))
	// dup

	// let f(n) =
	// a = {}
	// reverse(a)

	boolean hasAnyRef() {
		for (Ty t : this.getParamTypes()) {
			if (t.isAnyRef()) {
				return true;
			}
		}
		return false;
	}

	public Template generate(TEnv env) {
		Transpiler tr = env.getTranspiler();
		Template tp = tr.defineFunction(this.isPublic, this.name, this.funcId, this.paramNames, this.paramTypes,
				this.returnType, this.body);
		this.setExpired();
		return tp;
	}

	@Override
	public Template generate(TEnv env, Ty[] params) {
		this.used(env);
		if (!this.isAbstract()) {
			return this.generated;
		}
		VarDomain dom = new VarDomain(this.getParamNames());
		Ty[] p = dom.dupParamTypes(this.getParamTypes(), params);
		Transpiler tr = env.getTranspiler();
		String key = sigkey(this.name, this.funcId, p);
		Template tp = getGenerated(key);
		ODebug.trace("sigkey=%s %s", key, tp);
		if (tp == null) {
			tp = tr.defineFunction(this.isPublic, this.name, this.funcId, this.paramNames, p, this.getReturnType(),
					this.body);
			setGenerated(key, tp);
			tp.used(env);
		}
		return tp;
	}

	static String sigkey(String name, int id, Ty[] p) {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append("#");
		sb.append(name);
		for (Ty t : p) {
			sb.append(":");
			sb.append(t);
		}
		return sb.toString();
	}

	static HashMap<String, Template> sigkeyMap = new HashMap<>();

	static Template getGenerated(String sigkey) {
		return sigkeyMap.get(sigkey);
	}

	static void setGenerated(String sigkey, Template tp) {
		sigkeyMap.put(sigkey, tp);
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