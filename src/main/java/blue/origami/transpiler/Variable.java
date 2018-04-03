package blue.origami.transpiler;

import java.util.HashMap;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.VarNameCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import origami.nez2.Token;

public class Variable implements NameInfo {
	Token at;
	String name;
	int seq;
	int closureLevel = 0;
	Ty type;

	Variable(Token name, int seq, Ty type) {
		this.at = name;
		this.name = name.getSymbol();
		this.seq = seq;
		this.type = type;
	}

	Variable(String name, int seq, Ty type) {
		this.name = name;
		this.seq = seq;
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	public int getIndex() {
		return this.seq;
	}

	public Ty getType() {
		return this.type;
	}

	public int getLevel() {
		return this.closureLevel;
	}

	public void incRef() {
		this.closureLevel++;
	}

	public void decRef() {
		this.closureLevel--;
	}

	@Override
	public boolean isNameInfo(Env env) {
		return true;
	}

	@Override
	public Code newNameCode(Env env, Token s) {
		s = s == null ? this.at : s;
		if (this.closureLevel > 0) {

			HashMap<String, Code> closureMap = env.getFuncEnv().closureMap;
			closureMap.put(this.getName(),
					new VarNameCode(this.at, this.seq, this.type, this.closureLevel - 1).setSource(s));
		}
		return new VarNameCode(this.at, this.seq, this.type, this.closureLevel).setSource(s);
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