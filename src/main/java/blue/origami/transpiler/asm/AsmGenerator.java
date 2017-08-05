package blue.origami.transpiler.asm;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import blue.origami.konoha5.Func;
import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeTemplate;
import blue.origami.transpiler.TConstTemplate;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFuncType;
import blue.origami.transpiler.TFunction;
import blue.origami.transpiler.TGenerator;
import blue.origami.transpiler.TNameHint;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TExprCode;
import blue.origami.transpiler.code.TMultiCode;
import blue.origami.util.ODebug;

public class AsmGenerator extends TGenerator implements Opcodes {
	static AsmClassLoader classLoader = new AsmClassLoader();

	private ClassWriter cw;
	String cname;

	@Override
	protected void setup() {
		this.cname = "C$" + classLoader.seq();
		this.fieldSec = null;
		this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		this.cw.visit(V1_8, ACC_PUBLIC, this.cname, null/* signatrue */, "java/lang/Object", null);
		// this.cw.visitSource("<input>", null);

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
		AsmGenerator asm = env.get(AsmGenerator.class);
		if (asm == null) {
			env.add(AsmGenerator.class, this);
		}
		if (this.funcList != null) {
			for (TFunction f : this.funcList) {
				if (f.isGenerated()) {
					continue;
				}
				TCode body = f.getCode(env);
				this.defineFunction(env, f.isPublic(), f.getName(), f.getParamNames(), f.getParamTypes(),
						f.getReturnType(), body);
			}
			this.funcList = null;
		}
		if (code.isEmpty() && this.exampleList != null) {
			ArrayList<TCode> asserts = new ArrayList<>();
			for (Tree<?> t : this.exampleList) {
				TCode body = env.parseCode(env, t).asType(env, TType.tBool);
				asserts.add(new TExprCode("assert", body));
			}
			code = new TMultiCode(false, asserts.toArray(new TCode[asserts.size()])).asType(env, TType.tVoid);
			this.exampleList = null;
		}
		// ODebug.trace("isEmpty: %s %s", code.isEmpty(), code);
		if (!code.isEmpty()) {
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
		classLoader.set(this.cname, byteCode);
		try {
			Class<?> c = classLoader.loadClass(this.cname);
			java.lang.reflect.Method m = c.getMethod(evalName);
			return m.invoke(null);
		} catch (NoSuchMethodException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return e;
		}
	}

	@Override
	public TCodeTemplate newConstTemplate(TEnv env, String lname, TType ret) {
		StringBuilder sb = new StringBuilder();
		sb.append("F|");
		sb.append(this.cname);
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new TConstTemplate(lname, ret, template);
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, TType type, TCode expr) {
		AsmGenerator asm = env.get(AsmGenerator.class);
		if (asm == null) {
			env.add(AsmGenerator.class, this);
		}
		FieldNode fn = new FieldNode(ACC_PUBLIC + ACC_STATIC, name, toTypeDesc(type), null, null);
		fn.accept(this.cw);
		AsmSection sec = this.fieldSec();
		expr.emitCode(env, sec);
		sec.mBuilder.visitFieldInsn(PUTSTATIC, this.cname/* internal */, name, toTypeDesc(type));
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
			sec.addVariable(TNameHint.safeName(paramNames[i]) + i, paramTypes[i]);
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
	// static HashMap<Class<?>, TType> c2tMap = new HashMap<>();
	static HashMap<String, Class<?>> t2cMap = new HashMap<>();

	static void set(Class<?> c, TType t) {
		String key = t.toString();
		// c2tMap.put(c, t);
		t2cMap.put(key, c);
	}

	static Class<?> toClass(TType t) {
		String key = t.toString();
		Class<?> c = t2cMap.get(key);
		if (c == null) {
			if (t instanceof TFuncType) {
				c = loadFuncTypeClass(((TFuncType) t).getParamTypes(), ((TFuncType) t).getReturnType());
				set(c, t);
				return c;
			}
			if (t.isArrayType()) {
				return blue.origami.konoha5.ObjArray.class;
			}
			ODebug.trace("undefined type %s", t);
			// c = Object.class;
			assert (c != null);
		}
		return c;
	}

	static Type ti(TType t) {
		return Type.getType(toClass(t));
	}

	static Type[] ti(TType[] paramTypes) {
		Type[] p = new Type[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			p[i] = Type.getType(toClass(paramTypes[i]));
		}
		return p;
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

	// static TType toType(Class<?> c) {
	// return c2tMap.get(c);
	// }

	static {
		set(Object.class, TType.tVar("a"));
		set(Object.class, TType.tVar("b"));
		set(void.class, TType.tVoid);
		set(boolean.class, TType.tBool);
		set(char.class, TType.tChar);
		set(int.class, TType.tInt);
		set(double.class, TType.tFloat);
		set(String.class, TType.tString);
		set(blue.origami.konoha5.Data.class, TType.tData());
		set(blue.origami.konoha5.Data.class, TType.tData(TArrays.emptyNames));
		set(blue.origami.konoha5.IntArray.class, TType.tArray(TType.tInt));

		// Func
		set(Func.FuncBool.class, TType.tFunc(TType.tBool));
		set(Func.FuncBoolVoid.class, TType.tFunc(TType.tVoid, TType.tBool));
		set(Func.FuncBoolBool.class, TType.tFunc(TType.tBool, TType.tBool));
		set(Func.FuncBoolInt.class, TType.tFunc(TType.tInt, TType.tBool));
		set(Func.FuncBoolFloat.class, TType.tFunc(TType.tFloat, TType.tBool));
		set(Func.FuncBoolStr.class, TType.tFunc(TType.tString, TType.tBool));
		//
		set(Func.FuncInt.class, TType.tFunc(TType.tInt));
		set(Func.FuncIntVoid.class, TType.tFunc(TType.tVoid, TType.tInt));
		set(Func.FuncIntBool.class, TType.tFunc(TType.tBool, TType.tInt));
		set(Func.FuncIntInt.class, TType.tFunc(TType.tInt, TType.tInt));
		set(Func.FuncIntFloat.class, TType.tFunc(TType.tFloat, TType.tInt));
		set(Func.FuncIntStr.class, TType.tFunc(TType.tString, TType.tInt));
	}

	static Class<?> loadFuncTypeClass(TType[] paramTypes, TType returnType) {
		String cname1 = "T$" + classLoader.seq();
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw1.visit(V1_8, ACC_PUBLIC + +ACC_ABSTRACT + ACC_INTERFACE, cname1, null/* signatrue */, "java/lang/Object",
				null);

		Method m = new Method("apply", ti(returnType), ti(paramTypes));
		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT, m, null, null, cw1);
		mw.endMethod();
		cw1.visitEnd();

		classLoader.set(cname1, cw1.toByteArray());
		try {
			return classLoader.loadClass(cname1);
		} catch (ClassNotFoundException e) {
			ODebug.exit(1, e);
			return null;
		}
	}

	protected Class<?> loadFuncExprClass(TEnv env, String[] fieldNames, TType[] fieldTypes, int start,
			String[] paramNames, TType[] paramTypes, TType returnType, TCode body) {
		String cname1 = "C$" + classLoader.seq();
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		Class<?> funcType = toClass(TType.tFunc(returnType, paramTypes));
		cw1.visit(V1_8, ACC_PUBLIC, cname1, null/* signatrue */, "java/lang/Object",
				new String[] { Type.getInternalName(funcType) });

		for (int i = 0; i < fieldNames.length; i++) {
			FieldNode fn = new FieldNode(ACC_PUBLIC, fieldNames[i], toTypeDesc(fieldTypes[i]), null, null);
			fn.accept(cw1);
		}
		{
			MethodVisitor mv = cw1.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			Method m = new Method("apply", ti(returnType), ti(paramTypes));
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw1);
			AsmSection sec = new AsmSection(mw);
			sec.addVariable("this", TType.tFunc(returnType, paramTypes)); // FIXME
			for (int i = 0; i < paramNames.length; i++) {
				sec.addVariable(TNameHint.safeName(paramNames[i]) + (start + i), paramTypes[i]);
			}
			body.emitCode(env, sec);
			mw.returnValue();
			mw.endMethod();
		}
		cw1.visitEnd();
		classLoader.set(cname1, cw1.toByteArray());
		try {
			return classLoader.loadClass(cname1);
		} catch (ClassNotFoundException e) {
			ODebug.exit(1, e);
			return null;
		}
	}

}
