package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;

public class FunctionContext {

	FunctionContext parent;
	ArrayList<Variable> varList = new ArrayList<>();
	HashMap<String, Code> closureMap = null;

	public FunctionContext(FunctionContext parent) {
		this.parent = parent;
		if (parent != null) {
			this.closureMap = new HashMap<>();
		}
	}

	public Variable newVariable(String name, Ty type) {
		Variable v = new Variable(name, this.index(), type);
		this.varList.add(v);
		ODebug.trace("%s", v);
		return v;
	}

	private int index() {
		int index = this.varList.size();
		return (this.parent != null) ? index + this.parent.index() : index;
	}

	public int size() {
		return this.varList.size();
	}

	public Variable getFirstArgument() {
		return this.varList.get(0);
	}

	public int enter() {
		if (this.parent != null) {
			for (Variable v : this.parent.varList) {
				v.incRef();
			}
			this.parent.enter();
		}
		return this.index();
	}

	public HashMap<String, Code> exit() {
		if (this.parent != null) {
			for (Variable v : this.parent.varList) {
				v.decRef();
			}
			this.parent.exit();
		}
		return this.closureMap;
	}

	// public boolean isDuplicatedName(String name, Ty declType) {
	// for (Variable v : this.varList) {
	// if (name.equals(v.name) && declType.eq(v.type)) {
	// return true;
	// }
	// }
	// return false;
	// }

	public static class Variable implements NameInfo {
		String name;
		int seq;
		int closureLevel = 0;
		Ty type;

		Variable(String name, int seq, Ty type) {
			this.name = name;
			this.seq = seq;
			this.type = type;
		}

		public void incRef() {
			this.closureLevel++;
		}

		public void decRef() {
			this.closureLevel--;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean isNameInfo(TEnv env) {
			return true;
		}

		@Override
		public Code newCode(TEnv env, Tree<?> s) {
			if (this.closureLevel > 0) {
				HashMap<String, Code> closureMap = env.get(FunctionContext.class).closureMap;
				closureMap.put(this.getName(),
						new NameCode(this.name, this.seq, this.type, this.closureLevel - 1).setSource(s));
			}
			return new NameCode(this.name, this.seq, this.type, this.closureLevel).setSource(s);
		}

		@Override
		public String toString() {
			return String.format("[%s, %s :: %s, %s]", this.name, this.seq, this.type, this.closureLevel);
		}

		boolean isUsed = false;

		@Override
		public void used(TEnv env) {
			this.isUsed = true;
		}
	}

}
