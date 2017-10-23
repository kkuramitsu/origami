package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;

public class FunctionContext {

	FunctionContext parent;
	public int startIndex = 0;
	ArrayList<Variable> varList = new ArrayList<>();
	HashMap<String, Code> closureMap = null;

	public FunctionContext(FunctionContext parent) {
		this.parent = parent;
		if (parent != null) {
			this.startIndex = parent.index();
			this.closureMap = new HashMap<>();
		}
	}

	public FunctionContext() {
		this(null);
	}

	public Variable newVariable(AST name, Ty type) {
		Variable v = new Variable(name, this.index(), type);
		this.varList.add(v);
		return v;
	}

	public Variable newVariable(AST name, int index, Ty type) {
		if (index == -1) {
			Variable v = new Variable(name, this.index(), type);
			this.varList.add(v);
			return v;
		} else {
			Variable v = this.get(name.getString(), index);
			ODebug.trace("%s %s", v, type);
			return v;
		}
	}

	private Variable get(String name, int index) {
		if (index - this.startIndex >= 0) {
			return this.varList.get(index - this.startIndex);
		}
		return this.parent.get(name, index);
	}

	public void syncIndex(FunctionContext fcx) {
		this.startIndex = fcx.startIndex;
	}

	private int index() {
		return this.varList.size() + this.startIndex;
	}

	public int size() {
		return this.varList.size();
	}

	public Code getArgumentsPattern(Env env) {
		if (this.varList.size() == 1) {
			return this.varList.get(0).newNameCode(env, null);
		}
		return new TupleCode(this.varList.stream().map(v -> v.newNameCode(env, null)).toArray(Code[]::new));
	}

	public void enter() {
		if (this.parent != null) {
			for (Variable v : this.parent.varList) {
				v.incRef();
			}
			this.parent.enter();
		}
	}

	public void exit() {
		if (this.parent != null) {
			for (Variable v : this.parent.varList) {
				v.decRef();
			}
			this.parent.exit();
		}
	}

	public int getStartIndex() {
		return this.startIndex;
	}

	public String[] getFieldNames() {
		if (this.closureMap != null && this.closureMap.size() > 0) {
			return this.closureMap.keySet().toArray(new String[this.closureMap.size()]);
		}
		return OArrays.emptyNames;
	}

	public Code[] getFieldCode() {
		if (this.closureMap != null && this.closureMap.size() > 0) {
			Code[] p = new Code[this.closureMap.size()];
			int c = 0;
			for (String name : this.closureMap.keySet()) {
				p[c++] = this.closureMap.get(name);
			}
			return p;
		}
		return OArrays.emptyCodes;
	}

	public Ty[] getFieldTypes() {
		if (this.closureMap != null && this.closureMap.size() > 0) {
			Ty[] p = new Ty[this.closureMap.size()];
			int c = 0;
			for (String name : this.closureMap.keySet()) {
				p[c++] = this.closureMap.get(name).getType();
			}
			return p;
		}
		return OArrays.emptyTypes;
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
		AST at;
		String name;
		int seq;
		int closureLevel = 0;
		Ty type;

		Variable(AST name, int seq, Ty type) {
			this.at = name;
			this.name = name.getString();
			this.seq = seq;
			this.type = type;
		}

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

		public int getIndex() {
			return this.seq;
		}

		@Override
		public boolean isNameInfo(Env env) {
			return true;
		}

		@Override
		public Code newNameCode(Env env, AST s) {
			s = s == null ? this.at : s;
			if (this.closureLevel > 0) {
				HashMap<String, Code> closureMap = env.get(FunctionContext.class).closureMap;
				closureMap.put(this.getName(),
						new NameCode(this.at, this.seq, this.type, this.closureLevel - 1).setSource(s));
			}
			return new NameCode(this.at, this.seq, this.type, this.closureLevel).setSource(s);
		}

		@Override
		public String toString() {
			return String.format("[%s, %s :: %s, %s]", this.name, this.seq, this.type, this.closureLevel);
		}

		boolean isUsed = false;

		@Override
		public void used(Env env) {
			this.isUsed = true;
		}

	}

}
