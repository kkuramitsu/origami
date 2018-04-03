package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.FuncEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMatchContext;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarParamTy;
import origami.nez2.OStrings;
import origami.nez2.ParseTree;
import origami.nez2.Token;

public class FuncCode extends Code1 {

	Token[] paramNames;
	Ty[] paramTypes;
	Ty returnType;

	ParseTree body = null;
	FuncEnv funcEnv = null;

	public FuncCode(Token[] paramNames, Ty[] paramTypes, Ty returnType, ParseTree body) {
		super(new DoneCode());
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
		this.body = body;
		this.initVarTypes();
	}

	public FuncCode(Token[] paramNames, Ty[] paramTypes, Ty returnType, Code body) {
		super(body);
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
		this.body = null;
		this.initVarTypes();
	}

	private boolean isGeneric = false;

	private void initVarTypes() {
		if (OArrays.testSome(this.paramTypes, Ty.IsGeneric)) {
			VarDomain dom = new VarDomain(this.paramTypes);
			this.paramTypes = dom.conv(this.paramTypes);
			this.returnType = dom.conv(this.returnType);
			this.isGeneric = true;
		} else {
			if (this.returnType instanceof VarParamTy) {
				this.returnType = Ty.tVar(null);
			}
		}
	}

	// @Override
	// public String getName() {
	// return null;
	// }
	//
	// @Override
	public Token[] getParamNames() {
		return this.paramNames;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public Ty getReturnType() {
		return this.returnType;
	}

	public int getStartIndex() {
		return this.funcEnv.getStartIndex();
	}

	public String[] getFieldNames() {
		return this.funcEnv.getFieldNames();
	}

	public Code[] getFieldCode() {
		return this.funcEnv.getFieldCode();
	}

	public Ty[] getFieldTypes() {
		return this.funcEnv.getFieldTypes();
	}

	private FuncEnv getFuncEnv(Env env) {
		if (this.funcEnv == null) {
			this.funcEnv = env.newLambdaEnv(this);
		}
		return this.funcEnv;
	}

	public Code getCode(Env env) {
		return this.body != null ? env.parseCode(env, this.body) : this.getInner();
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			FuncEnv fcx = this.getFuncEnv(env);
			boolean stacked = fcx.tryTyping(OArrays.testSome(this.paramTypes, Ty.IsVar));
			this.inner = fcx.typeCheck(this.getCode(env));
			fcx.tryTyping(stacked);
			this.setType(Ty.tFunc(this.returnType, this.paramTypes));
			ODebug.trace("TODO1: %s as %s", this.getType(), ret);
		}
		if (ret.isFunc() && this.getType().hasSome(Ty.IsVar) /* && !ret.hasSome(Ty.IsGeneric) */) {
			FuncTy toFuncTy = (FuncTy) ret.devar();
			if (toFuncTy.getParamSize() != this.paramTypes.length) {
				throw new ErrorCode(this.getSource(), TFmt.mismatched_parameter_size_S_S, toFuncTy.getParamSize(),
						this.paramTypes.length);
			}
			ODebug.trace("TODO2: %s as %s typing=%s", this.getType(), toFuncTy, this.getType() != toFuncTy);
			if (this.getType() != toFuncTy) {
				for (int i = 0; i < this.paramTypes.length; i++) {
					this.paramTypes[i].match(TypeMatchContext.Update, false, toFuncTy.getParamTypes()[i]);
				}
				this.returnType = toFuncTy.getReturnType();
				FuncEnv fcx = this.getFuncEnv(env);
				fcx.update(this.paramTypes, this.returnType);
				this.inner = fcx.typeCheck(this.getCode(env));
				this.setType(Ty.tFunc(this.returnType, this.paramTypes));
				ODebug.trace("TODO2: %s as %s", this.getType(), ret);
			}
		}
		return this.castType(env, ret);
	}

	public static boolean isGenericFunc(Ty ty) {
		if (ty.isFunc()) {
			FuncTy funcTy = (FuncTy) ty.devar();
			return OArrays.testSome(funcTy.getParamTypes(), (t -> t.hasSome(Ty.IsGeneric)));
		}
		return false;
	}

	@Override
	public boolean showError(Env env) {
		if (!this.getInner().showError(env)) {
			FuncTy funcTy = (FuncTy) this.getType();
			// funcTy.isGeneric()
			if (isGenericFunc(funcTy)) {
				env.reportError(this.getSource(), TFmt.abstract_function_YY1__YY2, this, funcTy);
				return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushFuncExpr(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		for (int i = 0; i < this.paramNames.length; i++) {
			sb.append("\\");
			sb.append(this.paramNames[i]);
			sb.append(" ");
		}
		OStrings.append(sb, this.getInner());
		sb.append(")");
	}

	@Override
	public void dumpCode(SyntaxBuilder sb) {
		sb.TypeAnnotation(this.getType(), () -> {
			if (this.paramNames.length == 0) {
				sb.Keyword("\\() ");
			} else {
				for (int i = 0; i < this.paramNames.length; i++) {
					sb.Keyword("\\");
					sb.Name(this.paramNames[i].getSymbol());
					sb.append(" ");
				}
			}
			sb.Expr(this.getInner());
		});
	}

}
