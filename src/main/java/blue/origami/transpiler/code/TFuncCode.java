package blue.origami.transpiler.code;

import java.util.HashMap;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFunctionContext;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.ODebug;

public class TFuncCode extends Code1 {

	int startIndex;
	String[] paramNames;
	TType[] paramTypes;
	TType returnType;
	TType typed;
	HashMap<String, TCode> fieldMap = null;

	public TFuncCode(String[] paramNames, TType[] paramTypes, TType returnType, TCode inner) {
		super(inner);
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
		this.typed = TType.tUntyped;
	}

	public String[] getFieldNames() {
		if (this.fieldMap != null && this.fieldMap.size() > 0) {
			return this.fieldMap.keySet().toArray(new String[this.fieldMap.size()]);
		}
		return TArrays.emptyNames;
	}

	public TCode[] getFieldInitCode() {
		if (this.fieldMap != null && this.fieldMap.size() > 0) {
			TCode[] p = new TCode[this.fieldMap.size()];
			int c = 0;
			for (String name : this.fieldMap.keySet()) {
				p[c++] = this.fieldMap.get(name);
			}
			return p;
		}
		return TArrays.emptyCodes;
	}

	public TType[] getFieldTypes() {
		if (this.fieldMap != null && this.fieldMap.size() > 0) {
			TType[] p = new TType[this.fieldMap.size()];
			int c = 0;
			for (String name : this.fieldMap.keySet()) {
				p[c++] = this.fieldMap.get(name).getType();
			}
			return p;
		}
		return TArrays.emptyTypes;
	}

	public TType[] getParamTypes() {
		return this.paramTypes;
	}

	public TType getReturnType() {
		return this.returnType;
	}

	public int getStartIndex() {
		return this.startIndex;
	}

	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public TType getType() {
		return this.typed;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		ODebug.trace("asType...");
		if (this.typed.isUntyped()) {
			TEnv lenv = env.newEnv();
			TFunctionContext fcx = env.get(TFunctionContext.class);
			if (fcx == null) {
				fcx = new TFunctionContext();
				lenv.add(TFunctionContext.class, fcx);
			}
			HashMap<String, TCode> fieldMap = fcx.enterScope(new HashMap<>());
			this.startIndex = fcx.getStartIndex();
			for (int i = 0; i < this.paramNames.length; i++) {
				ODebug.trace("name=%s, %s", this.paramNames[i], this.paramTypes[i]);
				lenv.add(this.paramNames[i], fcx.newVariable(this.paramNames[i], this.paramTypes[i]));
			}
			this.inner = env.catchCode(() -> this.inner.asType(lenv, this.returnType));
			fieldMap = fcx.exitScope(fieldMap);
			if (this.returnType.isUntyped()) {
				assert (!this.returnType.isUntyped());
				return this;
			}
			this.typed = TType.tFunc(this.returnType, this.paramTypes);
			this.fieldMap = fieldMap;
			ODebug.trace("FuncCode.asType %s %s", this.typed, this.fieldMap);
		}
		return super.asType(env, t);
	}

	@Override
	public Template getTemplate(TEnv env) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushFuncExpr(env, this);
	}

}
