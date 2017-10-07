package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.util.ODebug;

public class TFunction extends CodeMap implements NameInfo, FuncUnit {
	static int seq = 0;
	private int funcId;
	protected boolean isPublic = false;
	AST at;
	protected AST[] paramNames;
	protected AST body;
	private CodeMap generated = null;

	public TFunction(boolean isPublic, AST name, Ty returnType, AST[] paramNames, Ty[] paramTypes, AST body) {
		super(0, name.getString(), "(uncompiled)", returnType, paramTypes);
		this.at = name;
		this.funcId = seq++;
		this.isPublic = isPublic;
		this.paramNames = paramNames;
		this.body = body;
	}

	public TFunction(AST name, Ty returnType, AST[] paramNames, Ty[] paramTypes, AST body) {
		this(false, name, returnType, paramNames, paramTypes, body);
	}

	public boolean isPublic() {
		return this.isPublic;
	}

	@Override
	public boolean isExpired() {
		return this.body == null;
	}

	void setExpired() {
		this.paramNames = null;
		this.body = null;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public String getDefined() {
		return this.generated == null ? "" : this.generated.getDefined();
	}

	@Override
	public AST getSource() {
		return this.at;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public AST[] getParamSource() {
		return this.paramNames;
	}

	@Override
	public void setParamTypes(Ty[] pats) {
		this.paramTypes = pats;
	}

	@Override
	public void setReturnType(Ty ret) {
		this.returnType = ret;
	}

	// let reverse(a) =
	// dup = {}
	// [0 to < |a|].forEach(\i dup.push(a[|a|-1-i]))
	// dup

	// let f(n) =
	// a = {}
	// reverse(a)

	@Override
	public void used(TEnv env) {
		if (this.isUnused()) {
			super.used(env);
			Code code = this.typeBody(env, new FunctionContext(), this.body);
			if (code == null) {
				this.setExpired();
				return;
			}
			if (TArrays.testSomeTrue(t -> t.hasVar(), this.getParamTypes())) {
				// this.isGeneric = true;
				ODebug.showBlue(TFmt.Template, () -> {
					ODebug.println("%s : %s", this.name, this.getFuncType());
				});
			} else {
				ODebug.trace("static %s %s generated=%s", this.name, this.getFuncType(), code.getType(),
						this.generated);
				// this.isGeneric = false;
				if (this.generated == null) {
					Transpiler tr = env.getTranspiler();
					boolean hasAbstract = this.isAbstract(code);
					this.generated = tr.defineFunction(this.isPublic, this.at, this.funcId, this.paramNames,
							this.paramTypes, this.returnType, null, hasAbstract ? env.parseCode(env, this.body) : code);
				}
				this.setExpired();
			}
		}
	}

	public CodeMap generate(TEnv env) {
		this.used(env);
		return this;
	}

	@Override
	public CodeMap generate(TEnv env, Ty[] params) {
		this.used(env);
		if (this.generated != null) {
			return this.generated;
		}
		if (this.body == null) {
			throw new ErrorCode(this.getSource(), TFmt.function_S_remains_undefined, this.name);
		}
		VarDomain dom = new VarDomain(this.getParamNames());
		Ty[] p = dom.dupParamTypes(this.getParamTypes(), params);
		Ty ret = dom.dupRetType(this.getReturnType());
		String key = polykey(this.name, this.funcId, p);
		CodeMap tp = getGenerated(key);
		ODebug.trace("sigkey=%s %s", key, tp);
		if (tp == null) {
			Transpiler tr = env.getTranspiler();
			ODebug.trace("Partial Evaluation: %s : %s => %s", this.name, this.getFuncType(), Ty.tFunc(ret, p));
			final CodeMap tp2 = tr.defineFunction(this.isPublic, this.getSource(), this.funcId, this.paramNames, p, ret,
					dom, this.body);
			ODebug.showBlue(TFmt.Template_Specialization, () -> {
				ODebug.println("%s : %s => %s", this.name, this.getFuncType(), tp2.getFuncType());
			});
			setGenerated(key, tp2);
			tp2.used(env);
			return tp2;
		}
		return tp;
	}

	static String polykey(String name, int id, Ty[] p) {
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

	static HashMap<String, CodeMap> polyMap = new HashMap<>();

	static CodeMap getGenerated(String sigkey) {
		return polyMap.get(sigkey);
	}

	static void setGenerated(String sigkey, CodeMap tp) {
		polyMap.put(sigkey, tp);
	}

	@Override
	public boolean isNameInfo(TEnv env) {
		return !this.isExpired();
	}

	@Override
	public Code newCode(TEnv env, AST s) {
		return new FuncRefCode(this.name, this).setSource(s);
	}

}