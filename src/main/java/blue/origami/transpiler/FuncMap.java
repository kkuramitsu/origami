package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.common.ODebug;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarParamTy;
import origami.nez2.ParseTree;
import origami.nez2.Token;

public class FuncMap extends CodeMap implements NameInfo/* , FuncUnit */ {
	static int seq = 0;

	static String newNameId(Token name) {
		return name.getSymbol() + (seq++);
	}

	//
	protected boolean isPublic = false;
	protected Token name;
	protected String nameId;
	protected Token[] paramNames;
	protected ParseTree body;
	private CodeMap generated = null;

	public FuncMap(boolean isPublic, Token name, Ty returnType, Token[] paramNames, Ty[] paramTypes, ParseTree body) {
		super(0, name.getSymbol(), "(uncompiled)", returnType, paramTypes);
		this.isPublic = isPublic;
		this.name = name;
		this.nameId = newNameId(name);
		this.paramNames = paramNames;
		this.body = body;
	}

	public FuncMap(Token name, Ty returnType, Token[] paramNames, Ty[] paramTypes, ParseTree body) {
		this(false, name, returnType, paramNames, paramTypes, body);
	}

	public FuncMap(Env env, Ty fromTy, Ty toTy, Token var, ParseTree body) {
		this(NameHint.getName("conv"), toTy, new Token[] { var }, new Ty[] { fromTy }, body);
		this.name = env.s(body);
	}

	public boolean isPublic() {
		return this.isPublic;
	}

	@Override
	public boolean isExpired() {
		return this.body == null;
	}

	void setExpired() {
		this.name = null;
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
	public String getName() {
		// Don't change to nameId;
		return this.name.getSymbol();
	}

	@Override
	public void setParamTypes(Ty[] pats) {
		this.paramTypes = pats;
	}

	@Override
	public void used(Env env) {
		if (this.isUnused()) {
			super.used(env);
			this.typeThis(env);
		}
	}

	void typeThis(Env env) {
		boolean IsUnspecificReturnType = this.getReturnType() instanceof VarParamTy;
		if (this.isGeneric()) {
			VarDomain dom = new VarDomain(this.getParamTypes());
			this.paramTypes = dom.conv(this.getParamTypes());
			this.returnType = dom.conv(this.getReturnType());
			FuncEnv fenv = env.newFuncEnv(this.nameId, this.paramNames, this.paramTypes, this.returnType);
			fenv.tryTyping(true);
			fenv.typeCheck(env.parseCode(env, this.body));
			dom.useParamVar();
			this.setParamTypes(Ty.map(this.getParamTypes(), ty -> {
				Ty ty2 = dom.conv(ty).memoed();
				// if (ty instanceof VarTy) {
				// boolean hasMutation = ty.hasMutation();
				// // System.out.printf("::::: Mutation=%s, %s => %s\n", hasMutation, ty, ty2);
				// if (!hasMutation && ty2.isMutable()) {
				// ODebug.trace("To Immutable %s", ty2);
				// ty2 = ty2.toImmutable();
				// }
				// }
				return ty2;
			}));
			int vars = dom.usedVars();
			this.returnType = dom.conv(this.returnType).memoed();
			if (vars == 0 && dom.usedVars() > vars) {
				throw new ErrorCode(this.name, TFmt.ambiguous_type__S, this.getReturnType());
			}
			if (this.isGeneric()) {
				ODebug.showBlue(TFmt.Template, () -> {
					ODebug.println("%s : %s", this.name, this.getFuncType());
				});
			}
		} else {
			if (this.returnType instanceof VarParamTy) {
				this.returnType = Ty.tVar(null);
			}
			FuncEnv fenv = env.newFuncEnv(this.nameId, this.paramNames, this.getParamTypes(), this.getReturnType());
			fenv.typeCheck(env.parseCode(env, this.body));
			if (this.returnType.hasSome(Ty.IsVar)) {
				throw new ErrorCode(this.name, TFmt.ambiguous_type__S, this.getReturnType());
			}
			this.returnType = this.returnType.memoed();
		}
		if (IsUnspecificReturnType) {
			this.returnType = this.returnType.toImmutable();
		}
	}

	public CodeMap generate(Env env) {
		this.used(env);
		if (this.generated != null) {
			return this.generated;
		}
		return this;
	}

	CodeMap generate(Env env, Code body) {
		if (this.generated != null) {
			return this.generated;
		}
		Transpiler tr = env.getTranspiler();
		CodeMap cmap = tr.defineFunction2(this.isPublic, this.getName(), this.nameId, this.paramNames,
				this.getParamTypes(), this.getReturnType(), body);
		return cmap;
	}

	@Override
	public CodeMap generate(Env env, Ty[] params) {
		this.used(env);
		if (this.generated != null) {
			return this.generated;
		}
		// if (this.body == null) {
		// throw new ErrorCode(this.getSource(), TFmt.function_S_remains_undefined,
		// this.name);
		// }
		FuncEnv fenv = env.getFuncEnv();
		if (fenv.IsTryTyping()) {
			return new CodeMap(0, "rec", ""/* abstract */, this.getReturnType(), this.getParamTypes());
		}
		if (!this.isGeneric()) {
			this.generated = this.generate(env, env.parseCode(env, this.body));
			return this.generated;
		}
		VarDomain dom = new VarDomain(this.getParamTypes());
		Ty[] p = dom.matched(this.getParamTypes(), params);
		Ty ret = dom.conv(this.getReturnType());
		String key = polyKey(this.nameId, p);
		CodeMap tp = getGenerated(key);
		// ODebug.trace("polykey=%s %s", key, tp);
		if (tp == null) {
			Transpiler tr = env.getTranspiler();
			// ODebug.trace("Partial Evaluation: %s : %s => %s", this.name,
			// this.getFuncType(), Ty.tFunc(ret, p));
			final CodeMap tp2 = tr.defineFunction2(this.isPublic, this.getName(), newNameId(this.name), this.paramNames,
					p, ret, env.parseCode(env, this.body));
			ODebug.showBlue(TFmt.Template_Specialization, () -> {
				ODebug.println("%s : %s => %s", this.name, this.getFuncType(), tp2.getFuncType());
			});
			setGenerated(key, tp2);
			tp2.used(env);
			return tp2;
		}
		return tp;

	}

	static String polyKey(String name, Ty[] p) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		for (Ty t : p) {
			sb.append(":");
			sb.append(t.memoed());
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
	public Code newNameCode(Env env, Token s) {
		return new FuncRefCode(this.name.getSymbol(), this).setSource(s);
	}

}