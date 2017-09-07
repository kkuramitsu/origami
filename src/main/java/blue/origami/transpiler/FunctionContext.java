package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;

public class FunctionContext {

	ArrayList<Variable> varList = new ArrayList<>();

	public Variable newVariable(String name, Ty type) {
		Variable v = new Variable(this.varList.size(), name, type);
		this.varList.add(v);
		return v;
	}

	public int size() {
		return this.varList.size();
	}

	public Variable getFirstArgument() {
		return this.varList.get(0);
	}

	public int getStartIndex() {
		return this.varList.size();
	}

	public boolean isDuplicatedName(String name, Ty declType) {
		for (Variable v : this.varList) {
			if (name.equals(v.name) && declType.eq(v.type)) {
				return true;
			}
		}
		return false;
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
		public Code newCode(Tree<?> s) {
			if (this.refLevel > 0 && FunctionContext.this.fieldMap != null) {
				FunctionContext.this.fieldMap.put(this.getName(),
						new NameCode(this.name, this.seq, this.type, this.refLevel - 1).setSource(s));
			}
			return new NameCode(this.name, this.seq, this.type, this.refLevel).setSource(s);
		}

		@Override
		public String toString() {
			return String.format("[%s, %s :: %s, %s]", this.name, this.seq, this.type, this.refLevel);
		}

		@Override
		public void used(TEnv env) {

		}
	}

}
