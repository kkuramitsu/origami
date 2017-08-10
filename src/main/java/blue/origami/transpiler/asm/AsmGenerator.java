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

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.Generator;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeTemplate;
import blue.origami.transpiler.TConstTemplate;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFunction;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.util.ODebug;

public class AsmGenerator extends Generator implements Opcodes {

	private ClassWriter cw;
	String cname;

	@Override
	protected void setup() {
		this.cname = "C$" + Asm.classLoader.seq();
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
			this.fieldSec = new AsmSection(this.cname, mw);
		}
		return this.fieldSec;
	}

	private static final Type[] emptyTypes = new Type[0];
	private static final String evalName = "eval";

	@Override
	public void emit(TEnv env, Code code) {
		AsmGenerator asm = env.get(AsmGenerator.class);
		if (asm == null) {
			env.add(AsmGenerator.class, this);
		}
		if (this.funcList != null) {
			for (TFunction f : this.funcList) {
				if (f.isExpired()) {
					continue;
				}
				f.generate(env);
			}
			this.funcList = null;
		}
		if (code.isEmpty() && this.exampleList != null) {
			ArrayList<Code> asserts = new ArrayList<>();
			for (Tree<?> t : this.exampleList) {
				Code body = env.parseCode(env, t).asType(env, Ty.tBool);
				asserts.add(new ExprCode("assert", body));
			}
			code = new MultiCode(false, asserts.toArray(new Code[asserts.size()])).asType(env, Ty.tVoid);
			this.exampleList = null;
		}
		// ODebug.trace("isEmpty: %s %s", code.isEmpty(), code);
		if (!code.isEmpty()) {
			Method m = new Method(evalName, Type.getType(Asm.toClass(code.getType())), emptyTypes);
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw);
			AsmSection sec = new AsmSection(this.cname, mw);
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
		Asm.classLoader.set(this.cname, byteCode);
		if (this.isVerbose()) {
			Asm.classLoader.dump(this.cname, byteCode);
		}
		try {
			Class<?> c = Asm.classLoader.loadClass(this.cname);
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
	public TCodeTemplate newConstTemplate(TEnv env, String lname, Ty ret) {
		StringBuilder sb = new StringBuilder();
		sb.append("F|");
		sb.append(this.cname);
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new TConstTemplate(lname, ret, template);
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr) {
		AsmGenerator asm = env.get(AsmGenerator.class);
		if (asm == null) {
			env.add(AsmGenerator.class, this);
		}
		FieldNode fn = new FieldNode(ACC_PUBLIC + ACC_STATIC, name, Asm.toTypeDesc(type), null, null);
		fn.accept(this.cw);
		AsmSection sec = this.fieldSec();
		expr.emitCode(env, sec);
		sec.mBuilder.visitFieldInsn(PUTSTATIC, this.cname/* internal */, name, Asm.toTypeDesc(type));
	}

	@Override
	public TCodeTemplate newFuncTemplate(TEnv env, String lname, Ty returnType, Ty... paramTypes) {
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
	public void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code) {
		Method m = new Method(name, Asm.ti(returnType), Asm.ti(paramTypes));
		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw);
		AsmSection sec = new AsmSection(this.cname, mw);
		for (int i = 0; i < paramNames.length; i++) {
			sec.addVariable(NameHint.safeName(paramNames[i]) + i, paramTypes[i]);
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

	static String nameApply(Ty t) {
		switch (t.toString()) {
		case "Bool":
			return "applyZ";
		case "Int":
			return "applyI";
		case "Float":
			return "applyD";
		case "String":
			return "applyS";
		}
		return "apply";
	}

	static Class<?> loadFuncTypeClass(Ty[] paramTypes, Ty returnType) {
		Method m = new Method(nameApply(returnType), Asm.ti(returnType), Asm.ti(paramTypes));
		System.out.println(":::::: " + m.toString());
		String cname1 = "T$" + Asm.classLoader.seq();
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw1.visit(V1_8, ACC_PUBLIC + +ACC_ABSTRACT + ACC_INTERFACE, cname1, null/* signatrue */, "java/lang/Object",
				null);

		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT, m, null, null, cw1);
		mw.endMethod();
		cw1.visitEnd();
		return loadClass(cname1, cw1);
	}

	private static Class<?> loadClass(String cname1, ClassWriter cw1) {
		Asm.classLoader.set(cname1, cw1.toByteArray());
		try {
			return Asm.classLoader.loadClass(cname1);
		} catch (ClassNotFoundException e) {
			ODebug.exit(1, e);
			return null;
		}
	}

	private static void addDefaultConstructor(ClassWriter cw1) {
		MethodVisitor mv = cw1.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	static Class<?> loadFuncExprClass(TEnv env, String[] fieldNames, Ty[] fieldTypes, int start, String[] paramNames,
			Ty[] paramTypes, Ty returnType, Code body) {
		String cname1 = "C$" + Asm.classLoader.seq();
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		Class<?> funcType = Asm.toClass(Ty.tFunc(returnType, paramTypes));
		cw1.visit(V1_8, ACC_PUBLIC, cname1, null/* signatrue */, "java/lang/Object",
				new String[] { Type.getInternalName(funcType) });

		for (int i = 0; i < fieldNames.length; i++) {
			FieldNode fn = new FieldNode(ACC_PUBLIC, fieldNames[i], Asm.toTypeDesc(fieldTypes[i]), null, null);
			fn.accept(cw1);
		}
		addDefaultConstructor(cw1);
		{
			Method m = new Method(nameApply(returnType), Asm.ti(returnType), Asm.ti(paramTypes));
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw1);
			AsmSection sec = new AsmSection(cname1, mw);
			sec.addVariable("this", Ty.tFunc(returnType, paramTypes)); // FIXME
			for (int i = 0; i < paramNames.length; i++) {
				sec.addVariable(NameHint.safeName(paramNames[i]) + (start + i), paramTypes[i]);
			}
			body.emitCode(env, sec);
			mw.returnValue();
			mw.endMethod();
		}
		cw1.visitEnd();
		return loadClass(cname1, cw1);
	}

	private static HashMap<String, Class<?>> refMap = null;

	static Class<?> loadFuncRefClass(TEnv env, Template tp) {
		if (refMap == null) {
			refMap = new HashMap<>();
		}
		String key = tp.toString();
		Class<?> c = refMap.get(key);
		if (c == null) {
			Ty returnType = tp.getReturnType();
			Ty[] paramTypes = tp.getParamTypes();
			Code[] p = new Code[paramTypes.length];
			String[] paramNames = new String[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++) {
				p[i] = new NameCode("a", i, paramTypes[i], 0);
				paramNames[i] = "a";
			}
			Code body = new ExprCode(tp, p);
			c = loadFuncExprClass(env, TArrays.emptyNames, TArrays.emptyTypes, 0, paramNames, paramTypes, returnType,
					body);
			refMap.put(key, c);
		}
		return c;
	}

	// // f: (int,int)->int, t: (double,double)->double
	// protected Class<?> loadFuncMapClass(TEnv env, TFuncType f, TFuncType t,
	// int mapCost) {
	// if (this.refMap == null) {
	// this.refMap = new HashMap<>();
	// }
	// String key = "(" + f + ")->(" + t + ")";
	// Class<?> c = this.refMap.get(key);
	// if (c == null) {
	// TType returnType = t.getReturnType();
	// TType[] paramTypes = t.getParamTypes();
	//
	// TCode[] fp = new TCode[f.getParamSize()];
	// String[] paramNames = new String[fp.length];
	// for (int i = 0; i < paramTypes.length; i++) {
	// fp[i] = new TNameCode("a" + i, f.getParamTypes()[i], 0);
	// paramNames[i] = "a";
	// }
	// TCode body = new TExprCode(tp, p);
	// c = this.loadFuncExprClass(env, TArrays.emptyNames, TArrays.emptyTypes,
	// 0, paramNames, paramTypes,
	// returnType, body);
	// this.refMap.put(key, c);
	// }
	// return c;
	// }

}
