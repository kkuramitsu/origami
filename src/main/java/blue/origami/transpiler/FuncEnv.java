package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.Token;

public class FuncEnv extends Env {
	protected final String nameId;
	protected Token[] paramNames;
	protected Ty[] paramTypes;
	protected Ty returnType;

	ArrayList<Variable> varList = new ArrayList<>();
	public int startIndex = 0;
	FuncEnv parent;
	HashMap<String, Code> closureMap = null;

	FuncEnv(Env env, FuncEnv parent, String nameId, Token[] paramNames, Ty[] paramTypes, Ty returnType) {
		super(env);
		this.nameId = nameId;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.returnType = returnType;
		this.parent = parent;
		if (parent != null) {
			this.startIndex = parent.newIndex();
			this.closureMap = new HashMap<>();
		} else if (nameId.equals("")) {
			this.isGlobal = true;
		}
	}

	private boolean isGlobal = false;

	public boolean isGlobalScope() {
		return false;
	}

	public Variable newVariable(Token name, Ty type) {
		Variable v = new Variable(name, this.newIndex(), type);
		this.varList.add(v);
		return v;
	}

	int newIndex() {
		return this.varList.size() + this.startIndex;
	}

	public Variable newVariable(Token name, int index, Ty type) {
		if (index == -1) {
			Variable v = new Variable(name, this.newIndex(), type);
			this.varList.add(v);
			return v;
		} else {
			Variable v = this.get(name.getSymbol(), index);
			ODebug.trace("%s %s", v, type);
			return v;
		}
	}

	Variable get(String name, int index) {
		if (index - this.startIndex >= 0) {
			return this.varList.get(index - this.startIndex);
		}
		return this.parent.get(name, index);
	}

	void enter() {
		if (this.parent != null) {
			for (Variable v : this.parent.varList) {
				v.incRef();
			}
			this.parent.enter();
		}
	}

	void exit() {
		if (this.parent != null) {
			for (Variable v : this.parent.varList) {
				v.decRef();
			}
			this.parent.exit();
		}
	}

	private boolean tryTyping = false;

	public boolean tryTyping(boolean b) {
		boolean t = this.tryTyping;
		this.tryTyping = b;
		return t;
	}

	public boolean IsTryTyping() {
		return this.tryTyping;
	}

	public Code typeCheck(Code code0) {
		this.enter();
		for (int i = 0; i < this.paramNames.length; i++) {
			String name = this.paramNames[i].getSymbol();
			this.add(name, this.newVariable(this.paramNames[i], this.paramTypes[i]));
		}
		Code code = this.catchCode(() -> code0.asType(this, this.returnType));
		this.exit();
		return code;
	}

	/* Match, Pattern */

	public int getParamSize() {
		return this.varList.size();
	}

	public Code getArgumentsPattern(Env env) {
		if (this.varList.size() == 1) {
			return this.varList.get(0).newNameCode(env, null);
		}
		return new TupleCode(this.varList.stream().map(v -> v.newNameCode(env, null)).toArray(Code[]::new));
	}

	/* Closure */

	public void update(Ty[] paramTypes, Ty returnType) {
		this.paramTypes = paramTypes;
		this.returnType = returnType;
		this.varList = new ArrayList<>();
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

}
