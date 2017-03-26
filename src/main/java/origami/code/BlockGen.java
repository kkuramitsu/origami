package origami.code;

import java.util.ArrayList;
import java.util.List;

import origami.asm.code.ArrayGetCode;
import origami.lang.OEnv;
import origami.lang.OLocalVariable;
import origami.lang.OMethod;
import origami.lang.type.OType;
import origami.util.ODebug;
import origami.util.OTypeUtils;

public class BlockGen {
	final OEnv env;
	final int rand;
	List<OCode> codes = new ArrayList<>();

	public BlockGen(OEnv env) {
		this.env = env.newEnv();
		this.rand = (int) (Math.random() * 10000);
	}

	public BlockGen(BlockGen block) {
		this.env = block.env().newEnv();
		this.rand = block.rand;
	}

	public OEnv env() {
		return this.env;
	}

	public OCode eEmpty() {
		return new OEmptyCode(this.env());
	}

	public OCode eValue(Object v) {
		return this.env().v(v);
	}

	public OCode eNull() {
		return new ONullCode(this.env().t(Object.class));
	}

	public String realName(String name) {
		return name.endsWith("_") ? name + this.rand : name;
	}

	public ONameCode eVar(String name) {
		name = this.realName(name);
		OLocalVariable v = this.env().get(name, OLocalVariable.class);
		return new ONameCode(name, v.getType());
	}

	public ONameCode eVar(String name, Class<?> c) {
		name = this.realName(name);
		return new ONameCode(name, this.env().t(c));
	}

	public OCode eBin(OCode left, String op, OCode right) {
		OCode c = left.newBinaryCode(this.env(), op, right);
		return c;
	}

	public OCode eMethod(OCode base, String name, OCode... params) {
		return base.newMethodCode(this.env(), name, params);
	}

	public OCode eGetIndex(OCode base, int index) {
		return new ArrayGetCode(base, this.eValue(index));
	}

	public OCode eNew(Class<?> c, OCode... params) {
		OType t = this.env().t(c);
		return t.newConstructorCode(this.env(), params);
	}

	public OCode eIf(OCode cond, OCode then, OCode elsec) {
		return new OIfCode(this.env(), cond, then, elsec);
	}

	public OCode desugar() {
		if (this.codes.size() == 0) {
			return new OEmptyCode(this.env());
		}
		if (this.codes.size() == 1) {
			return this.codes.get(0);
		}
		return new OMultiCode(this.codes.toArray(new OCode[this.codes.size()]));
	}

	/* push */

	public void push(OCode c) {
		this.codes.add(c);
	}

	// public OCode assign(String name, OCode right) {
	// return new OAssignCode(this.env().t(void.class), false,
	// this.realName(name), right);
	// }

	public void pushAssign(String name, OCode right) {
		this.push(new OAssignCode(this.env().t(void.class), false, this.realName(name), right));
	}

	// public OCode define(String name, Class<?> c) {
	// OType t = this.env().t(c);
	// name = this.realName(name);
	// this.env().add(name, new OLocalVariable(true, name, t));
	// return new OAssignCode(this.env().t(void.class), true, name, new
	// ODefaultValueCode(t));
	// }

	public void pushDefine(String name, OCode right) {
		name = this.realName(name);
		this.env().add(name, new OLocalVariable(true, name, right.getType()));
		this.push(new OAssignCode(this.env().t(void.class), true, name, right));
	}

	public void pushDefine(String name, Class<?> c) {
		this.pushDefine(name, this.env().t(c));
	}

	public void pushDefine(String name, OType t) {
		this.pushDefine(name, new ODefaultValueCode(t));
	}

	public void pushIf(OCode cond, OCode thenCode, OCode elseCode) {
		this.push(this.eIf(cond, thenCode, elseCode));
	}

	public void pushThrow(OCode expr) {
		this.push(new OThrowCode(this.env(), expr));
	}

	/* Debug */
	public void p(OCode p) {
		OMethod m = new OMethod(this.env(), OTypeUtils.loadMethod(BlockGen.class, "_p", Object.class));
		this.push(m.newMethodCode(this.env(), p.boxCode(this.env())));
	}

	public void p(Object v) {
		OMethod m = new OMethod(this.env(), OTypeUtils.loadMethod(BlockGen.class, "_p", Object.class));
		this.push(m.newMethodCode(this.env(), this.env().v(v)));
	}

	public final static void _p(Object o) {
		ODebug.trace("debug %s", o);
	}
}
