package blue.origami.asm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import blue.origami.konoha5.Data$;
import blue.origami.konoha5.Func;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.TupleTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMap;
import blue.origami.transpiler.type.VarTy;
import blue.origami.util.ODebug;
import blue.origami.util.OStrings;

public class AsmType extends TypeMap<Class<?>> implements Opcodes {
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
		// this.reg(Ty.tUntyped0, Object.class);
		this.reg(new VarTy("a", 0), Object.class);
		this.reg(Ty.tAny, Object.class);
		this.reg(Ty.tVoid, void.class);
		this.reg(Ty.tBool, boolean.class);
		this.reg(Ty.tChar, char.class);
		this.reg(Ty.tInt, int.class);
		this.reg(Ty.tFloat, double.class);
		this.reg(Ty.tString, String.class);

		this.reg("Option", Object.class);

		this.reg("{}", blue.origami.konoha5.Data$.class);
		this.reg("Data$", blue.origami.konoha5.Data$.class);

		this.reg("List", blue.origami.konoha5.List$.class);
		this.reg("List'", blue.origami.konoha5.List$.class);
		this.reg("ListI", blue.origami.konoha5.List$Int.class);
		this.reg("List'I", blue.origami.konoha5.List$Int.class);

		this.reg("Stream", Stream.class);
		this.reg("StreamI", IntStream.class);
		this.reg("StreamD", DoubleStream.class);

		this.reg("Stream'", Stream.class);
		this.reg("Stream'I", IntStream.class);
		this.reg("Stream'D", DoubleStream.class);

		this.reg("Dict", blue.origami.konoha5.Dict$.class);
		this.reg("Dict'", blue.origami.konoha5.Dict$.class);

		// Func
		this.reg("V->Z", Func.FuncBool.class);
		this.reg("V->I", Func.FuncInt.class);
		this.reg("V->D", Func.FuncFloat.class);
		this.reg("V->O", Func.FuncObj.class);

		this.reg("I->V", Func.FuncIntVoid.class);
		this.reg("I->Z", Func.FuncIntBool.class);
		this.reg("I->I", Func.FuncIntInt.class);
		this.reg("I->D", Func.FuncIntFloat.class);
		this.reg("I->O", Func.FuncIntObj.class);
		this.reg("II->I", Func.FuncIntIntInt.class);
		//
		this.reg("D->V", Func.FuncFloatVoid.class);
		this.reg("D->Z", Func.FuncFloatBool.class);
		this.reg("D->I", Func.FuncFloatInt.class);
		this.reg("D->O", Func.FuncFloatObj.class);
		this.reg("D->D", Func.FuncFloatFloat.class);
		this.reg("DD->D", Func.FuncFloatFloatFloat.class);
		//
		this.reg("O->V", Func.FuncObjVoid.class);
		this.reg("O->Z", Func.FuncObjBool.class);
		this.reg("O->I", Func.FuncObjInt.class);
		this.reg("O->D", Func.FuncObjFloat.class);
		this.reg("O->O", Func.FuncObjObj.class);
		this.reg("OO->O", Func.FuncObjObjObj.class);
	}

	@Override
	public Class<?> type(Ty ty) {
		Class<?> c = ty.finalTy().mapType(this);
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

	String descFunc(Ty t) {
		String s = this.desc(t);
		return s.equals("Ljava/lang/Object;") ? "O" : s;
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
	protected String keyForeignFuncType(FuncTy funcTy) {
		StringBuilder sb = new StringBuilder();
		OStrings.joins(sb, Arrays.stream(funcTy.getParamTypes()).map(ty -> this.descFunc(ty)).toArray(String[]::new),
				"");
		sb.append("->");
		sb.append(this.descFunc(funcTy.getReturnType()));
		return sb.toString();
	}

	@Override
	protected Class<?> genForeignFuncType(FuncTy funcTy) {
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
	protected String keyForeignTupleType(TupleTy tupleTy) {
		StringBuilder sb = new StringBuilder();
		OStrings.joins(sb, Arrays.stream(tupleTy.getParamTypes()).map(ty -> this.descFunc(ty)).toArray(String[]::new),
				"");
		return sb.toString();
	}

	@Override
	protected Class<?> genForeignTupleType(TupleTy tupleTy) {
		String cname1 = "T$" + classLoader.seq();
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw1.visit(V1_8, ACC_PUBLIC, cname1, null/* signatrue */, Type.getInternalName(Data$.class), null);
		for (int i = 0; i < tupleTy.getParamSize(); i++) {
			FieldNode fn = new FieldNode(ACC_PUBLIC, "_" + i, this.desc(tupleTy.getParamTypes()[i]), null, null);
			fn.accept(cw1);
		}
		addDefaultConstructor(cw1, Object.class);
		cw1.visitEnd();
		return loadClass(cname1, cw1);
	}

	@Override
	protected String keyForeignDataType(DataTy dataTy) {
		return dataTy.size() == 0 ? "{}" : OStrings.joins(dataTy.names(), ",");
	}

	@Override
	protected Class<?> genForeingDataType(DataTy dataTy) {
		String[] names = dataTy.names();
		String cname1 = "D$" + this.seq();
		String[] infs = Arrays.stream(names).map(x -> Type.getInternalName(this.gen(x))).toArray(String[]::new);
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
		classLoader.store(cname, cw.toByteArray());
		try {
			return classLoader.loadClass(cname);
		} catch (ClassNotFoundException e) {
			ODebug.exit(1, e);
			return null;
		}
	}

	static String nameApply(Ty t) {
		switch (t.finalTy().toString()) {
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
			FieldNode fn = new FieldNode(ACC_PUBLIC, fieldNames[i] + i, this.desc(fieldTypes[i]), null, null);
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

	Class<?> loadFuncRefClass(TEnv env, CodeMap tp) {
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
		String[] names = dataTy.names();
		String cname1 = "Data$" + OStrings.joins(names, "");
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
