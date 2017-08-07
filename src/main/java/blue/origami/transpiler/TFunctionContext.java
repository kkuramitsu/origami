package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.rule.NameExpr.TNameRef;

public class TFunctionContext {

	ArrayList<TVariable> varList = new ArrayList<>();

	public boolean isDuplicatedName(String name, Ty declType) {
		for (TVariable v : this.varList) {
			if (name.equals(v.name) && declType.eq(v.type)) {
				return true;
			}
		}
		return false;
	}

	public TVariable newVariable(String name, Ty type) {
		TVariable v = new TVariable(this.varList.size(), name, type);
		this.varList.add(v);
		return v;
	}

	public int getStartIndex() {
		return this.varList.size();
	}

	HashMap<String, Code> fieldMap = null;

	public HashMap<String, Code> enterScope(HashMap<String, Code> fieldMap) {
		HashMap<String, Code> backMap = this.fieldMap;
		this.fieldMap = fieldMap;
		for (TVariable v : this.varList) {
			v.incRef();
		}
		return backMap;
	}

	public HashMap<String, Code> exitScope(HashMap<String, Code> fieldMap) {
		HashMap<String, Code> backMap = this.fieldMap;
		this.fieldMap = fieldMap;
		for (TVariable v : this.varList) {
			if (v.refLevel > 0) {
				v.decRef();
			}
		}
		return backMap;
	}

	public class TVariable implements TNameRef {
		int refLevel = 0;
		int index;
		String name;
		Ty type;

		TVariable(int index, String name, Ty type) {
			this.index = index;
			this.name = name;
			this.type = type;
		}

		public void incRef() {
			this.refLevel++;
		}

		public void decRef() {
			this.refLevel--;
		}

		public String getName() {
			return TNameHint.safeName(this.name) + this.index;
		}

		@Override
		public boolean isNameRef(TEnv env) {
			return true;
		}

		@Override
		public Code nameCode(TEnv env, String name) {
			// ODebug.trace("capture %s %d", name, this.refLevel);
			if (this.refLevel > 0 && TFunctionContext.this.fieldMap != null) {
				TFunctionContext.this.fieldMap.put(this.getName(),
						new NameCode(this.getName(), this.type, this.refLevel - 1));
			}
			return new NameCode(this.getName(), this.type, this.refLevel);
		}
	}

}
