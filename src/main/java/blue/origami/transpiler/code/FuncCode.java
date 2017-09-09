package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.FuncParam;
import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.FunctionUnit;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.OStrings;

public class FuncCode extends Code1 implements FuncParam, FunctionUnit {

	String[] paramNames;
	Ty[] paramTypes;
	Ty returnType;

	Tree<?> body = null;
	FunctionContext funcContext = null;

	public FuncCode(String[] paramNames, Ty[] paramTypes, Ty returnType, Tree<?> body) {
		super(new DeclCode());
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
		this.body = body;
	}

	public FuncCode(String[] paramNames, Ty[] paramTypes, Ty returnType, Code body) {
		super(body);
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
		this.body = null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	@Override
	public Ty getReturnType() {
		return this.returnType;
	}

	@Override
	public void setParamTypes(Ty[] pats) {
		this.paramTypes = pats;
	}

	@Override
	public void setReturnType(Ty ret) {
		this.returnType = ret;
	}

	private FunctionContext sync(TEnv env) {
		FunctionContext fcx = new FunctionContext(env.get(FunctionContext.class));
		if (this.funcContext != null) {
			fcx.syncIndex(this.funcContext);
		}
		this.funcContext = fcx;
		return fcx;
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			FunctionContext fcx = this.sync(env);
			Code inner = this.body != null ? env.parseCode(env, this.body) : this.getInner();
			this.inner = this.typeBody(env, fcx, inner);
			this.setType(Ty.tFunc(this.returnType, this.paramTypes));
		}
		if (ret.isFunc() && !this.getType().eq(ret)) {
			ODebug.trace(() -> ODebug.p("%s as %s", this.getType(), ret));
			FunctionContext fcx = this.sync(env);
			Code inner = this.body != null ? env.parseCode(env, this.body) : this.getInner();
			this.inner = this.typeBody(env, fcx, inner);
		}
		return this.castType(env, ret);
	}

	@Override
	public int getStartIndex() {
		return this.funcContext.getStartIndex();
	}

	public String[] getFieldNames() {
		return this.funcContext.getFieldNames();
	}

	public Code[] getFieldCode() {
		return this.funcContext.getFieldCode();
	}

	public Ty[] getFieldTypes() {
		return this.funcContext.getFieldTypes();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushFuncExpr(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		for (int i = 0; i < this.paramNames.length; i++) {
			sb.append("\\");
			sb.append(this.paramNames[i]);
			sb.append(" ");
		}
		sb.append("-> ");
		OStrings.append(sb, this.getInner());
		sb.append(")");
	}

}
