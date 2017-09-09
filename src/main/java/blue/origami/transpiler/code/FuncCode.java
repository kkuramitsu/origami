package blue.origami.transpiler.code;

import java.util.HashMap;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.FuncParam;
import blue.origami.transpiler.FunctionUnit;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.StringCombinator;

public class FuncCode extends Code1 implements FuncParam, FunctionUnit {

	String[] paramNames;
	Ty[] paramTypes;
	Ty returnType;

	Tree<?> body;
	int startIndex;
	HashMap<String, Code> fieldMap = null;

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
			this.inner = this.typeBody(env, this.body);
			this.setType(Ty.tFunc(this.returnType, this.paramTypes));
		}
		if (ret.isFunc() && !this.getType().eq(ret)) {

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

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setParamTypes(Ty[] pats) {
		this.paramTypes = pats;
	}

	@Override
	public void setReturnType(Ty ret) {
		this.returnType = ret;
	}

	@Override
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	@Override
	public void setFieldMap(HashMap<String, Code> fieldMap) {
		this.fieldMap = fieldMap;
	}

}
