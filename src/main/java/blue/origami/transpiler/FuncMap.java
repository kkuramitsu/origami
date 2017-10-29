package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;

public class FuncMap extends CodeMap implements NameInfo, FuncUnit {
	static int seq = 0;
	private int funcId;
	protected boolean isPublic = false;
	protected AST at;
	protected AST[] paramNames;
	protected AST body;
	private CodeMap generated = null;

	public FuncMap(boolean isPublic, AST name, Ty returnType, AST[] paramNames, Ty[] paramTypes, AST body) {
		super(0, name.getString(), "(uncompiled)", returnType, paramTypes);
		this.at = name;
		this.funcId = seq++;
		this.isPublic = isPublic;
		this.paramNames = paramNames;
		this.body = body;
	}

	public FuncMap(AST name, Ty returnType, AST[] paramNames, Ty[] paramTypes, AST body) {
		this(false, name, returnType, paramNames, paramTypes, body);
	}

	public FuncMap(Ty fromTy, Ty toTy, AST var, AST body) {
		this(AST.getName("conv"), toTy, new AST[] { var }, new Ty[] { fromTy }, body);
		this.at = body;
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

	boolean isTyping = false;

	@Override
	public void used(Env env) {
		if (this.isUnused()) {
			super.used(env);
			this.isTyping = true;
			Code code = this.typeBody(env, new FunctionContext(), this.body);
			this.isTyping = false;
			if (code == null) {
				this.setExpired();
				return;
			}
			if (OArrays.testSomeTrue(t -> t.hasSome(Ty.IsGeneric), this.getParamTypes())) {
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

	public CodeMap generate(Env env) {
		this.used(env);
		return this;
	}

	@Override
	public CodeMap generate(Env env, Ty[] params) {
		this.used(env);
		if (this.generated != null) {
			return this.generated;
		}
		if (this.body == null) {
			throw new ErrorCode(this.getSource(), TFmt.function_S_remains_undefined, this.name);
		}
		if (this.isTyping) {
			// return env.getTranspiler().newCodeMap(this.getName(), this.getReturnType(),
			// this.getParamTypes());
			return new CodeMap(0, "rec", ""/* abstract */, this.getReturnType(), this.getParamTypes());
		}
		VarDomain dom = new VarDomain(this.getParamNames());
		Ty[] p = dom.matched(this.getParamTypes(), params);
		Ty ret = dom.conv(this.getReturnType());
		String key = polykey(this.name, this.funcId, p);
		CodeMap tp = getGenerated(key);
		ODebug.trace("polykey=%s %s", key, tp);
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
	public boolean isNameInfo(Env env) {
		return !this.isExpired();
	}

	@Override
	public Code newNameCode(Env env, AST s) {
		return new FuncRefCode(this.name, this).setSource(s);
	}

}