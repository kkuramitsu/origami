package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;

public class TFunction extends Template implements NameInfo {
	static int seq = 0;
	private int id;
	protected boolean isPublic = false;
	protected String[] paramNames;
	protected Tree<?> body;
	// private VarDomain dom;

	public TFunction(boolean isPublic, String name, Ty returnType, String[] paramNames, Ty[] paramTypes, Tree<?> body) {
		super(name, returnType, paramTypes);
		this.id = seq++;
		this.isPublic = isPublic;
		this.paramNames = paramNames;
		this.body = body;
	}

	public boolean isPublic() {
		return this.isPublic;
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
		Template tp = tr.defineFunction(this.isPublic, this.name, this.id, this.paramNames, this.paramTypes,
				this.returnType, this.body);
		this.setExpired();
		return tp;
	}

	@Override
	public Template generate(TEnv env, Ty[] params) {
		if (!this.hasAnyRef()) {
			return this.generate(env);
		}
		Ty[] p = this.getParamTypes().clone();
		for (int i = 0; i < p.length; i++) {
			if (p[i].isAnyRef()) {
				p[i] = params[i];
			}
		}
		Transpiler tr = env.getTranspiler();
		String key = sigkey(this.name, this.id, p);
		Template tp = getGenerated(key);
		ODebug.trace("sigkey=%s %s", key, tp);
		if (tp == null) {
			tp = tr.defineFunction(this.isPublic, this.name, this.id, this.paramNames, p, this.getReturnType(),
					this.body);
			setGenerated(key, tp);
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