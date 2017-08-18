package blue.origami.transpiler.asm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import blue.origami.konoha5.Data$;
import blue.origami.konoha5.Func;
import blue.origami.transpiler.CodeType;
import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.FuncTy;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class AsmType extends CodeType<Class<?>> implements Opcodes {
	static AsmClassLoader classLoader = new AsmClassLoader();

	public AsmType(TEnv env) {
		super(env);
		this.loadType();
	}

	@Override
	public int seq() {
		return classLoader.seq();
	}

	void loadType() {
		this.reg(Ty.tUntyped0, Object.class);
		this.reg(Ty.tVar("a"), Object.class);
		this.reg(Ty.tVoid, void.class);
		this.reg(Ty.tBool, boolean.class);
		this.reg(Ty.tChar, char.class);
		this.reg(Ty.tInt, int.class);
		this.reg(Ty.tFloat, double.class);
		this.reg(Ty.tString, String.class);
		this.reg("{}", blue.origami.konoha5.Data$.class);
		this.reg("Data$", blue.origami.konoha5.Data$.class);

		this.reg("ListI", blue.origami.konoha5.List$Int.class);
		this.reg("List'I", blue.origami.konoha5.List$Int.class);
		this.reg("List", blue.origami.konoha5.List$.class);
		this.reg("List'", blue.origami.konoha5.List$.class);

		this.reg(Ty.tOption(Ty.tInt), Integer.class);
		this.reg(Ty.tList(Ty.tFloat), Double.class);

		// Func
		this.reg(Ty.tFunc(Ty.tBool), Func.FuncBool.class);
		this.reg(Ty.tFunc(Ty.tVoid, Ty.tBool), Func.FuncBoolVoid.class);
		this.reg(Ty.tFunc(Ty.tBool, Ty.tBool), Func.FuncBoolBool.class);
		this.reg(Ty.tFunc(Ty.tInt, Ty.tBool), Func.FuncBoolInt.class);
		this.reg(Ty.tFunc(Ty.tFloat, Ty.tBool), Func.FuncBoolFloat.class);
		this.reg(Ty.tFunc(Ty.tString, Ty.tBool), Func.FuncBoolStr.class);
		//
		this.reg(Ty.tFunc(Ty.tInt), Func.FuncInt.class);
		this.reg(Ty.tFunc(Ty.tVoid, Ty.tInt), Func.FuncIntVoid.class);
		this.reg(Ty.tFunc(Ty.tBool, Ty.tInt), Func.FuncIntBool.class);
		this.reg(Ty.tFunc(Ty.tInt, Ty.tInt), Func.FuncIntInt.class);
		this.reg(Ty.tFunc(Ty.tFloat, Ty.tInt), Func.FuncIntFloat.class);
		this.reg(Ty.tFunc(Ty.tString, Ty.tInt), Func.FuncIntStr.class);
	}

	@Override
	public Class<?> type(Ty ty) {
		Class<?> c = ty.mapType(this);
		assert (c != null) : "undefined type " + ty + " @" + ty.getClass().getName();
		return c;
	}

	@Override
	public Class<?>[] types(Ty... ty) {
		return Arrays.stream(ty).map(t -> this.type(t)).toArray(Class[]::new);
	}

	public Class<?> toClass(Ty ty) {
		return this.type(ty);
	}

	Type ti(Ty t) {
		return Type.getType(this.type(t));
	}

	Type[] ts(Ty... paramTypes) {
		return Arrays.stream(paramTypes).map(t -> this.ti(t)).toArray(Type[]::new);
	}

	String desc(Ty t) {
		return this.ti(t).getDescriptor();
	}

	String desc(Ty ret, Ty... paramTypes) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Ty t : paramTypes) {
			sb.append(this.desc(t));
		}
		sb.append(")");
		sb.append(this.desc(ret));
		return sb.toString();
	}

	@Override
	public String key(Class<?> c) {
		return Type.getDescriptor(c);
	}

	@Override
	protected Class<?> mapDefaultType(String name) {
		assert (false) : "undefined type: " + name;
		return Object.class;
	}

	@Override
	protected Class<?> mapDefaultType(String prefix, Ty ty, Class<?> inner) {
		Class<?> c = this.typeMap.get(prefix);
		assert (c != null) : "undefined " + prefix;
		return c;
	}

	@Override
	protected String key(FuncTy funcTy) {
		StringBuilder sb = new StringBuilder();
		StringCombinator.joins(sb,
				Arrays.stream(funcTy.getParamTypes()).map(ty -> this.desc(ty)).toArray(String[]::new), "");
		sb.append("->");
		sb.append(this.desc(funcTy.getReturnType()));
		return sb.toString();
	}

	@Override
	protected Class<?> gen(FuncTy funcTy) {
		Method m = new Method(nameApply(funcTy.getReturnType()), this.ti(funcTy.getReturnType()),
				this.ts(funcTy.getParamTypes()));
		String cname1 = "T$" + classLoader.seq();
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw1.visit(V1_8, ACC_PUBLIC + +ACC_ABSTRACT + ACC_INTERFACE, cname1, null/* signatrue */, "java/lang/Object",
				null);
		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT, m, null, null, cw1);
		mw.endMethod();
		cw1.visitEnd();
		return loadClass(cname1, cw1);
	}

	@Override
	protected String key(DataTy dataTy) {
		return dataTy.size() == 0 ? "{}" : dataTy.names().toString();
	}

	@Override
	protected Class<?> gen(DataTy dataTy) {
		Set<String> names = dataTy.names();
		String cname1 = "D$" + this.seq();
		String[] infs = names.stream().map(x -> Type.getInternalName(this.gen(x))).toArray(String[]::new);
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw1.visit(V1_8, ACC_PUBLIC + +ACC_ABSTRACT + ACC_INTERFACE, cname1, null/* signatrue */, "java/lang/Object",
				infs);
		for (String name : names) {
			Type type = this.ti(this.fieldTy(name));
			Method getm = new Method(name, type, this.ts(TArrays.emptyTypes));
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT, getm, null, null, cw1);
			mw.endMethod();
			Method setm = new Method(name, Type.VOID_TYPE, new Type[] { type });
			mw = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT, setm, null, null, cw1);
			mw.endMethod();
		}
		cw1.visitEnd();
		return loadClass(cname1, cw1);
	}

	String nameFieldClass(String name) {
		return "D$" + name;
	}

	Class<?> gen(String name) {
		String cname1 = this.nameFieldClass(name);
		return this.reg(cname1, () -> {
			ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			cw1.visit(V1_8, ACC_PUBLIC + +ACC_ABSTRACT + ACC_INTERFACE, cname1, null/* signatrue */, "java/lang/Object",
					null);
			Ty type = this.fieldTy(name);
			{
				Method m = new Method(name, this.ti(type), this.ts(TArrays.emptyTypes));
				GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT, m, null, null, cw1);
				mw.endMethod();
			}
			{
				Method m = new Method(name, Type.VOID_TYPE, this.ts(type));
				GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT, m, null, null, cw1);
				mw.endMethod();
			}
			cw1.visitEnd();
			return loadClass(cname1, cw1);
		});
	}

	private static void addDefaultConstructor(ClassWriter cw1, String superClass) {
		MethodVisitor mv = cw1.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private static void addDefaultConstructor(ClassWriter cw1, Class<?> superClass) {
		addDefaultConstructor(cw1, Type.getInternalName(superClass));
	}

	private static Class<?> loadClass(String cname, ClassWriter cw) {
		classLoader.set(cname, cw.toByteArray());
		try {
			return classLoader.loadClass(cname);
		} catch (ClassNotFoundException e) {
			ODebug.exit(1, e);
			return null;
		}
	}

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

	Class<?> loadFuncExprClass(TEnv env, String[] fieldNames, Ty[] fieldTypes, int start, String[] paramNames,
			Ty[] paramTypes, Ty returnType, Code body) {
		String cname1 = "C$" + this.seq();
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		Class<?> funcType = this.toClass(Ty.tFunc(returnType, paramTypes));
		cw1.visit(V1_8, ACC_PUBLIC, cname1, null/* signatrue */, "java/lang/Object",
				new String[] { Type.getInternalName(funcType) });

		for (int i = 0; i < fieldNames.length; i++) {
			FieldNode fn = new FieldNode(ACC_PUBLIC, fieldNames[i], this.desc(fieldTypes[i]), null, null);
			fn.accept(cw1);
		}
		addDefaultConstructor(cw1, Object.class);
		{
			Method m = new Method(nameApply(returnType), this.ti(returnType), this.ts(paramTypes));
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw1);
			AsmSection sec = new AsmSection(this, cname1, mw);
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

	private HashMap<String, Class<?>> refMap = null;

	Class<?> loadFuncRefClass(TEnv env, Template tp) {
		if (this.refMap == null) {
			this.refMap = new HashMap<>();
		}
		String key = tp.toString();
		Class<?> c = this.refMap.get(key);
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
			c = this.loadFuncExprClass(env, TArrays.emptyNames, TArrays.emptyTypes, 0, paramNames, paramTypes,
					returnType, body);
			this.refMap.put(key, c);
		}
		return c;
	}

	Class<?> loadDataClass(DataTy dataTy) {
		Set<String> names = dataTy.names();
		String cname1 = "Data$" + StringCombinator.joins(names.toArray(new String[names.size()]), "");
		return this.reg(cname1, () -> {
			Class<?> c = this.toClass(dataTy);
			ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			cw1.visit(V1_8, ACC_PUBLIC, cname1, null/* signatrue */, Type.getInternalName(Data$.class),
					new String[] { Type.getInternalName(c) });
			addDefaultConstructor(cw1, Data$.class);
			for (String name : names) {
				Ty ty = this.fieldTy(name);
				Type type = this.ti(ty);
				FieldNode fn = new FieldNode(ACC_PUBLIC, name, this.desc(ty), null, null);
				fn.accept(cw1);
				Method getm = new Method(name, type, this.ts(TArrays.emptyTypes));
				GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_FINAL, getm, null, null, cw1);
				mw.loadThis();
				mw.getField(Type.getType("L" + cname1 + ";"), name, type);
				mw.returnValue();
				mw.endMethod();
				Method setm = new Method(name, Type.VOID_TYPE, new Type[] { type });
				mw = new GeneratorAdapter(ACC_PUBLIC + ACC_FINAL, setm, null, null, cw1);
				mw.loadThis();
				mw.loadArg(0);
				mw.putField(Type.getType("L" + cname1 + ";"), name, type);
				mw.returnValue();
				mw.endMethod();
			}
			cw1.visitEnd();
			return loadClass(cname1, cw1);
		});
	}

	// static
	private final static Map<Class<?>, Class<?>> boxMap = new HashMap<>();
	// private final static Map<Class<?>, Class<?>> unboxMap = new HashMap<>();
	//
	// static {
	// unboxMap.put(Boolean.class, boolean.class);
	// unboxMap.put(Byte.class, byte.class);
	// unboxMap.put(Character.class, char.class);
	// unboxMap.put(Short.class, short.class);
	// unboxMap.put(Integer.class, int.class);
	// unboxMap.put(Float.class, float.class);
	// unboxMap.put(Long.class, long.class);
	// unboxMap.put(Double.class, double.class);
	// }

	static {
		boxMap.put(boolean.class, Boolean.class);
		boxMap.put(byte.class, Byte.class);
		boxMap.put(char.class, Character.class);
		boxMap.put(short.class, Short.class);
		boxMap.put(int.class, Integer.class);
		boxMap.put(float.class, Float.class);
		boxMap.put(long.class, Long.class);
		boxMap.put(double.class, Double.class);
	}

	public final static Class<?> boxType(Class<?> c) {
		return boxMap.getOrDefault(c, c);
	}

	public static boolean isDouble(Ty t) {
		return t.eq(Ty.tFloat) || t.eq(Ty.tInt64);
	}

}
