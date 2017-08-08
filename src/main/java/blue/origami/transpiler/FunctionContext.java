package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;

public class FunctionContext {

	ArrayList<Variable> varList = new ArrayList<>();

	public boolean isDuplicatedName(String name, Ty declType) {
		for (Variable v : this.varList) {
			if (name.equals(v.name) && declType.eq(v.type)) {
				return true;
			}
		}
		return false;
	}

	public Variable newVariable(String name, Ty type) {
		Variable v = new Variable(this.varList.size(), name, type);
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
		for (Variable v : this.varList) {
			v.incRef();
		}
		return backMap;
	}

	public HashMap<String, Code> exitScope(HashMap<String, Code> fieldMap) {
		HashMap<String, Code> backMap = this.fieldMap;
		this.fieldMap = fieldMap;
		for (Variable v : this.varList) {
			if (v.refLevel > 0) {
				v.decRef();
			}
		}
		return backMap;
	}

	public class Variable implements NameInfo {
		String name;
		int seq;
		int refLevel = 0;
		Ty type;

		Variable(int seq, String name, Ty type) {
			this.name = name;
			this.seq = seq;
			this.type = type;
		}

		public void incRef() {
			this.refLevel++;
		}

		public void decRef() {
			this.refLevel--;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean isNameInfo(TEnv env) {
			return true;
		}

		@Override
		public Code nameCode(TEnv env, String name) {
			if (this.refLevel > 0 && FunctionContext.this.fieldMap != null) {
				FunctionContext.this.fieldMap.put(this.getName(),
						new NameCode(this.name, this.seq, this.type, this.refLevel - 1));
			}
			return new NameCode(this.name, this.seq, this.type, this.refLevel);
		}
	}

}
