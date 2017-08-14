// package blue.origami.transpiler.asm;
//
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.Map;
//
// import org.objectweb.asm.Type;
//
// import blue.origami.konoha5.Func;
// import blue.origami.transpiler.ArrayTy;
// import blue.origami.transpiler.FuncTy;
// import blue.origami.transpiler.OptionTy;
// import blue.origami.transpiler.TArrays;
// import blue.origami.transpiler.Ty;
// import blue.origami.util.ODebug;
//
// public class Asm {
// static AsmClassLoader classLoader = new AsmClassLoader();
// static HashMap<String, Class<?>> t2cMap = new HashMap<>();
//
// static Class<?> toClass(Ty ty) {
// ty = ty.nomTy();
// String key = ty.key();
// Class<?> c = t2cMap.get(key);
// if (c == null) {
// if (ty instanceof OptionTy) {
// return Object.class;
// }
// if (ty instanceof FuncTy) {
// c = AsmGenerator.loadFuncTypeClass(((FuncTy) ty).getParamTypes(), ((FuncTy)
// ty).getReturnType());
// set(c, ty);
// return c;
// }
// if (ty instanceof ArrayTy) {
// return blue.origami.konoha5.ObjArray.class;
// }
// ODebug.trace("undefined type %s %s", ty, ty.getClass());
// // c = Object.class;
// assert (c != null);
// }
// return c;
// }
//
// static Type ti(Ty t) {
// return Type.getType(toClass(t));
// }
//
// static Type[] ts(Ty... paramTypes) {
// return Arrays.stream(paramTypes).map(t ->
// Type.getType(toClass(t))).toArray(Type[]::new);
// }
//
// static String toTypeDesc(Ty t) {
// return Type.getType(toClass(t)).getDescriptor();
// }
//
// static String toTypeDesc(Ty ret, Ty... paramTypes) {
// StringBuilder sb = new StringBuilder();
// sb.append("(");
// for (Ty t : paramTypes) {
// sb.append(toTypeDesc(t));
// }
// sb.append(")");
// sb.append(toTypeDesc(ret));
// return sb.toString();
// }
//
// // static TType toType(Class<?> c) {
// // return c2tMap.get(c);
// // }
//
// static void set(Class<?> c, Ty t) {
// t2cMap.put(t.key(), c);
// }
//
// static {
// set(Object.class, Ty.tUntyped0);
// set(Object.class, Ty.tVar("a"));
// set(void.class, Ty.tVoid);
// set(boolean.class, Ty.tBool);
// set(char.class, Ty.tChar);
// set(int.class, Ty.tInt);
// set(double.class, Ty.tFloat);
// set(String.class, Ty.tString);
// set(blue.origami.konoha5.Data.class, Ty.tData());
// set(blue.origami.konoha5.Data.class, Ty.tData(TArrays.emptyNames));
// set(blue.origami.konoha5.IntArray.class, Ty.tImArray(Ty.tInt));
// set(blue.origami.konoha5.IntArray.class, Ty.tArray(Ty.tInt));
//
// set(Integer.class, Ty.tOption(Ty.tInt));
// set(Double.class, Ty.tArray(Ty.tFloat));
//
// // Func
// set(Func.FuncBool.class, Ty.tFunc(Ty.tBool));
// set(Func.FuncBoolVoid.class, Ty.tFunc(Ty.tVoid, Ty.tBool));
// set(Func.FuncBoolBool.class, Ty.tFunc(Ty.tBool, Ty.tBool));
// set(Func.FuncBoolInt.class, Ty.tFunc(Ty.tInt, Ty.tBool));
// set(Func.FuncBoolFloat.class, Ty.tFunc(Ty.tFloat, Ty.tBool));
// set(Func.FuncBoolStr.class, Ty.tFunc(Ty.tString, Ty.tBool));
// //
// set(Func.FuncInt.class, Ty.tFunc(Ty.tInt));
// set(Func.FuncIntVoid.class, Ty.tFunc(Ty.tVoid, Ty.tInt));
// set(Func.FuncIntBool.class, Ty.tFunc(Ty.tBool, Ty.tInt));
// set(Func.FuncIntInt.class, Ty.tFunc(Ty.tInt, Ty.tInt));
// set(Func.FuncIntFloat.class, Ty.tFunc(Ty.tFloat, Ty.tInt));
// set(Func.FuncIntStr.class, Ty.tFunc(Ty.tString, Ty.tInt));
// }
//
// public final static Class<?> boxType(Class<?> c) {
// return boxMap.getOrDefault(c, c);
// }
//
// private final static Map<Class<?>, Class<?>> boxMap = new HashMap<>();
// // private final static Map<Class<?>, Class<?>> unboxMap = new HashMap<>();
// //
// // static {
// // unboxMap.put(Boolean.class, boolean.class);
// // unboxMap.put(Byte.class, byte.class);
// // unboxMap.put(Character.class, char.class);
// // unboxMap.put(Short.class, short.class);
// // unboxMap.put(Integer.class, int.class);
// // unboxMap.put(Float.class, float.class);
// // unboxMap.put(Long.class, long.class);
// // unboxMap.put(Double.class, double.class);
// // }
//
// static {
// boxMap.put(boolean.class, Boolean.class);
// boxMap.put(byte.class, Byte.class);
// boxMap.put(char.class, Character.class);
// boxMap.put(short.class, Short.class);
// boxMap.put(int.class, Integer.class);
// boxMap.put(float.class, Float.class);
// boxMap.put(long.class, Long.class);
// boxMap.put(double.class, Double.class);
// }
//
// }
