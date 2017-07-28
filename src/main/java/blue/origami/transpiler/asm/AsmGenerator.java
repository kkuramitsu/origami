package blue.origami.transpiler.asm;

import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import blue.origami.transpiler.TCodeTemplate;
import blue.origami.transpiler.TConstTemplate;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TGenerator;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.TCode;
import blue.origami.util.ODebug;

public class AsmGenerator extends TGenerator implements Opcodes {
	AsmClassLoader classLoader = new AsmClassLoader();
	int seq = 0;

	private ClassWriter cw;
	String cname;

	@Override
	protected void setup() {
		this.cname = "C$" + (this.seq++);
		this.fieldSec = null;
		this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		this.cw.visit(V1_8, ACC_PUBLIC, this.cname, null/* signatrue */, "java/lang/Object", null);
		this.cw.visitSource("<input>", null);
	}

	AsmSection fieldSec;

	private AsmSection fieldSec() {
		if (this.fieldSec == null) {
			Method m = Method.getMethod("void <clinit>()");
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw);
			this.fieldSec = new AsmSection(mw);
		}
		return this.fieldSec;
	}

	private static final Type[] emptyTypes = new Type[0];
	private static final String evalName = "eval";

	@Override
	public void emit(TEnv env, TCode code) {
		Method m = new Method(evalName, Type.getType(toClass(code.getType())), emptyTypes);
		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw);
		AsmSection sec = new AsmSection(mw);
		try {
			code.emitCode(env, sec);
		} catch (Exception e) {
			ODebug.traceException(e);
		}
		mw.returnValue();
		mw.endMethod();
	}

	@Override
	protected Object wrapUp() {
		if (this.fieldSec != null) {
			this.fieldSec.mBuilder.returnValue();
			this.fieldSec.mBuilder.endMethod();
			this.fieldSec = null;
		}
		this.cw.visitEnd();
		byte[] byteCode = this.cw.toByteArray();
		this.classLoader.set(this.cname, byteCode);
		try {
			Class<?> c = this.classLoader.loadClass(this.cname);
			java.lang.reflect.Method m = c.getMethod(evalName);
			return m.invoke(null);
		} catch (Exception e) {
			e.printStackTrace();
			return e;
		}
	}

	@Override
	public TCodeTemplate newConstTemplate(TEnv env, String lname, TType returnType) {
		StringBuilder sb = new StringBuilder();
		sb.append("F|");
		sb.append(this.cname);
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new TConstTemplate(lname, returnType, template);
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, TType type, TCode expr) {
		FieldNode fn = new FieldNode(ACC_PUBLIC + ACC_STATIC, name, toTypeDesc(type), null, null);
		fn.accept(this.cw);
		AsmSection sec = this.fieldSec();
		expr.emitCode(env, sec);
		sec.mBuilder.visitFieldInsn(Opcodes.PUTSTATIC, this.cname/* internal */, name, toTypeDesc(type));
	}

	@Override
	public TCodeTemplate newFuncTemplate(TEnv env, String lname, TType returnType, TType... paramTypes) {
		// this.mw.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt",
		// "(D)D", false);
		StringBuilder sb = new StringBuilder();
		sb.append("S|");
		sb.append(this.cname);
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new TCodeTemplate(lname, returnType, paramTypes, template);
	}

	@Override
	public void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, TType[] paramTypes,
			TType returnType, TCode code) {
		Type[] p = new Type[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			p[i] = Type.getType(toClass(paramTypes[i]));
		}
		Method m = new Method(name, Type.getType(toClass(returnType)), p);
		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw);
		AsmSection sec = new AsmSection(mw);
		for (int i = 0; i < paramNames.length; i++) {
			sec.addVariable(paramNames[i] + i, paramTypes[i]);
		}
		try {
			code.emitCode(env, sec);
		} catch (Exception e) {
			ODebug.traceException(e);
		}
		mw.returnValue();
		mw.endMethod();
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
		Class<?> c = t2cMap.get(key);
		if (c == null) {
			ODebug.trace("undefined type %s", t);
			c = Object.class;
		}
		return c;
	}

	static String toTypeDesc(TType t) {
		return Type.getType(toClass(t)).getDescriptor();
	}

	static String toTypeDesc(TType ret, TType... paramTypes) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (TType t : paramTypes) {
			sb.append(toTypeDesc(t));
		}
		sb.append(")");
		sb.append(toTypeDesc(ret));
		return sb.toString();
	}

	static TType toType(Class<?> c) {
		return c2tMap.get(c);
	}

	static {
		set(void.class, TType.tVoid);
		set(boolean.class, TType.tBool);
		set(int.class, TType.tInt);
		set(double.class, TType.tFloat);
		set(String.class, TType.tString);
	}

}
