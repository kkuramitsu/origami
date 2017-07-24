package blue.origami.transpiler.asm;

import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TGenerator;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.TCode;

public class TAsm extends TGenerator implements Opcodes {
	ClassWriter cw;

	@Override
	protected void setup() {
		this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		// cw.visit(V1_8, cdecl.getAnno().acc(), this.cname,
		// cdecl.getSignature(), superType.getInternalName(), inames);
		// cw.visitSource(cdecl.getSourceName(), null);

	}

	@Override
	protected void wrapUp() {
		this.cw.visitEnd();
		this.cw.toByteArray();
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, TType type, TCode expr) {

	}

	@Override
	public void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, TType[] paramTypes,
			TType returnType, TCode code) {
		// String params = "";
		// if (paramTypes.length > 0) {
		// String delim = env.getSymbolOrElse(",", ",");
		// StringBuilder sb = new StringBuilder();
		// sb.append(env.format("param", "%1$s %2$s", paramTypes[0].strOut(env),
		// paramNames[0] + 0));
		// for (int i = 1; i < paramTypes.length; i++) {
		// sb.append(delim);
		// sb.append(env.format("param", "%1$s %2$s", paramTypes[i].strOut(env),
		// paramNames[i] + i));
		// }
		// params = sb.toString();
		// }
		// SourceSection sec = new SourceSection();
		// this.sectionMap.put(name, sec);
		// this.currentFuncName = name;
		// sec.pushLine(env.format("function", "%1$s %2$s(%3$s) {",
		// returnType.strOut(env), name, params));
		// sec.incIndent();
		// sec.pushLine(env.format("return", "%s", code.strOut(env)));
		// sec.decIndent();
		// sec.pushLine(env.getSymbol("end function", "end", "}"));
	}

	// local
	static HashMap<Class<?>, TType> c2tMap = new HashMap<>();
	static HashMap<String, Class<?>> t2cMap = new HashMap<>();

	static void set(Class<?> c, TType t) {
		String key = t.toString();
		c2tMap.put(c, t);
		t2cMap.put(key, c);
	}

	static Class<?> toClass(TType t) {
		String key = t.toString();
		return t2cMap.get(key);
	}

	static TType toType(Class<?> c) {
		return c2tMap.get(c);
	}

	static {
		set(boolean.class, TType.tBool);
		set(int.class, TType.tInt);
		set(double.class, TType.tFloat);
		set(String.class, TType.tString);
	}
}
