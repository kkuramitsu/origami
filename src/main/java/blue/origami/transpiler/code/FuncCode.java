package blue.origami.transpiler.code;

import java.util.HashMap;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.FuncParam;
import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class FuncCode extends Code1 implements FuncParam {

	int startIndex;
	String[] paramNames;
	Ty[] paramTypes;
	Ty returnType;
	Tree<?> body;
	HashMap<String, Code> fieldMap = null;

	public FuncCode(String[] paramNames, Ty[] paramTypes, Ty returnType, Code body) {
		super(body);
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
	}

	@Override
	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public Ty getReturnType() {
		return this.returnType;
	}

	@Override
	public int getStartIndex() {
		return this.startIndex;
	}

	public String[] getFieldNames() {
		if (this.fieldMap != null && this.fieldMap.size() > 0) {
			return this.fieldMap.keySet().toArray(new String[this.fieldMap.size()]);
		}
		return TArrays.emptyNames;
	}

	public Code[] getFieldCode() {
		if (this.fieldMap != null && this.fieldMap.size() > 0) {
			Code[] p = new Code[this.fieldMap.size()];
			int c = 0;
			for (String name : this.fieldMap.keySet()) {
				p[c++] = this.fieldMap.get(name);
			}
			return p;
		}
		return TArrays.emptyCodes;
	}

	public Ty[] getFieldTypes() {
		if (this.fieldMap != null && this.fieldMap.size() > 0) {
			Ty[] p = new Ty[this.fieldMap.size()];
			int c = 0;
			for (String name : this.fieldMap.keySet()) {
				p[c++] = this.fieldMap.get(name).getType();
			}
			return p;
		}
		return TArrays.emptyTypes;
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			TEnv lenv = env.newEnv();
			FunctionContext fcx = env.get(FunctionContext.class);
			if (fcx == null) {
				fcx = new FunctionContext();
				lenv.add(FunctionContext.class, fcx);
			}
			HashMap<String, Code> fieldMap = fcx.enterScope(new HashMap<>());
			this.startIndex = fcx.getStartIndex();
			for (int i = 0; i < this.paramNames.length; i++) {
				lenv.add(this.paramNames[i], fcx.newVariable(this.paramNames[i], this.paramTypes[i]));
			}
			this.inner = env.catchCode(() -> this.inner.asType(lenv, this.returnType));
			fieldMap = fcx.exitScope(fieldMap);
			this.setType(Ty.tFunc(this.returnType, this.paramTypes));
			this.fieldMap = fieldMap;
		}
		if (ret.isFunc()) {

		}
		return this.castType(env, ret);
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
		StringCombinator.append(sb, this.getInner());
		sb.append(")");
	}

}
