package blue.origami.transpiler.asm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import blue.origami.konoha5.Func;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TFuncType;
import blue.origami.transpiler.TType;
import blue.origami.util.ODebug;

public class Asm {
	static AsmClassLoader classLoader = new AsmClassLoader();
	static HashMap<String, Class<?>> t2cMap = new HashMap<>();

	static void set(Class<?> c, TType t) {
		String key = t.toString();
		t2cMap.put(key, c);
	}

	static Class<?> toClass(TType t) {
		t = t.realType();
		String key = t.toString();
		Class<?> c = t2cMap.get(key);
		if (c == null) {
			if (t instanceof TFuncType) {
				c = AsmGenerator.loadFuncTypeClass(((TFuncType) t).getParamTypes(), ((TFuncType) t).getReturnType());
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
		set(Object.class, TType.tUntyped);
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

	public final static Class<?> boxType(Class<?> c) {
		return boxMap.getOrDefault(c, c);
	}

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

}
