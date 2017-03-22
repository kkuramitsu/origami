package origami.code;

import java.util.ArrayList;
import java.util.List;

import origami.asm.code.ArrayGetCode;
import origami.lang.OEnv;
import origami.lang.OLocalVariable;
import origami.lang.OMethod;
import origami.lang.type.OType;
import origami.lang.type.OUntypedType;
import origami.util.ODebug;
import origami.util.OTypeUtils;

public class GenerativeCode extends OSugarCode {
	List<OCode> codes = new ArrayList<>();
	final int rand;

	public GenerativeCode(OEnv env, GenerativeCode gcode) {
		super(env, env.t(OUntypedType.class));
		this.rand = gcode == null ? (int) (Math.random() * 10000) : gcode.rand;
	}

	public GenerativeCode(OEnv env, GenerativeCode gcode, OCode... params) {
		super(env, env.t(OUntypedType.class), params);
		this.rand = gcode == null ? (int) (Math.random() * 10000) : gcode.rand;
	}

	@Override
	public OEnv env() {
		return this.getHandled();
	}

	// public void setenv(OEnv cEnv) {
	// this.env = cEnv;
	// }

	public OCode _empty() {
		return new OEmptyCode(env());
	}

	public OCode _value(Object v) {
		return env().v(v);
	}

	public OCode _null() {
		return new ONullCode(env().t(Object.class));
	}

	public String name(String name) {
		return name.endsWith("_") ? name + this.rand : name;
	}

	public ONameCode _var(String name) {
		name = name(name);
		OLocalVariable v = env().get(name, OLocalVariable.class);
		return new ONameCode(name, v.getType());
	}

	public ONameCode _var(String name, Class<?> c) {
		name = name(name);
		return new ONameCode(name, env().t(c));
	}

	public OCode _bin(OCode left, String op, OCode right) {
		OCode c = left.newBinaryCode(env(), op, right);
		return c;
	}

	public OCode _method(OCode base, String name, OCode... params) {
		return base.newMethodCode(env(), name, params);
	}

	public OCode _geti(OCode base, int index) {
		return new ArrayGetCode(base, _value(index));
	}

	public OCode _new(Class<?> c, OCode... params) {
		OType t = env().t(c);
		return t.newConstructorCode(env(), params);
	}

	public OCode _if(OCode cond, OCode then, OCode elsec) {
		return new OIfCode(env(), cond, then, elsec);
	}

	private String label = null;
	private String resultName = null;
	private OCode initRightCode = null;

	public void setLabel(String label, String name, OCode right) {
		this.label = name(label);
		if (name != null) {
			this.resultName = name(name);
			this.initRightCode = right;
			env().add(resultName, new OLocalVariable(true, resultName, right.getType()));
		}
	}

	public OCode multiCode() {
		if (codes.size() == 0) {
			return new OEmptyCode(env());
		}
		if (codes.size() == 1) {
			return codes.get(0);
		}
		return new OMultiCode(codes.toArray(new OCode[codes.size()]));
	}

	@Override
	public OCode desugar() {
		OCode multiCode = multiCode();
		if (label != null) {
			OCode initCode = resultName != null ? new OAssignCode(env().t(void.class), true, resultName, initRightCode)
					: _empty();
			OCode thusCode = resultName != null ? new ONameCode(resultName, initRightCode.getType()) : _empty();
			// ODebug.trace("thusCode=%s", thusCode);
			return new OLabelBlockCode(label, initCode, multiCode, thusCode);
		}
		return multiCode();
	}

	/* push */

	public void push(OCode c) {
		codes.add(c);
	}

	public OCode assign(String name, OCode right) {
		return new OAssignCode(env().t(void.class), false, name(name), right);
	}

	public void pushAssign(String name, OCode right) {
		push(new OAssignCode(env().t(void.class), false, name(name), right));
	}

	public OCode define(String name, Class<?> c) {
		OType t = env().t(c);
		name = name(name);
		env().add(name, new OLocalVariable(true, name, t));
		return new OAssignCode(env().t(void.class), true, name, new ODefaultValueCode(t));
	}

	public void pushDefine(String name, OCode right) {
		name = name(name);
		env().add(name, new OLocalVariable(true, name, right.getType()));
		push(new OAssignCode(env().t(void.class), true, name, right));
	}

	public void pushDefine(String name, Class<?> c) {
		pushDefine(name, env().t(c));
	}

	public void pushDefine(String name, OType t) {
		pushDefine(name, new ODefaultValueCode(t));
	}

	public void pushIf(OCode cond, OCode thenCode, OCode elseCode) {
		push(_if(cond, thenCode, elseCode));
	}

	public void pushThrow(OCode expr) {
		push(new OThrowCode(env(), expr));
	}

	/* Debug */
	public void p(OCode p) {
		OMethod m = new OMethod(env(), OTypeUtils.loadMethod(GenerativeCode.class, "_p", Object.class));
		push(m.newMethodCode(env(), p.boxCode(env())));
	}

	public void p(Object v) {
		OMethod m = new OMethod(env(), OTypeUtils.loadMethod(GenerativeCode.class, "_p", Object.class));
		push(m.newMethodCode(env(), env().v(v)));
	}

	public final static void _p(Object o) {
		ODebug.trace("debug %s", o);
	}
}
