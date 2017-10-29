// package blue.origami.transpiler.type;
//
// import blue.origami.transpiler.CodeMap;
// import blue.origami.transpiler.Env;
// import blue.origami.transpiler.code.CastCode;
//
// class AnyTy extends SimpleTy {
//
// AnyTy() {
// super("AnyRef");
// }
//
// @Override
// public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
// return codeTy == this;
// }
//
// @Override
// public int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
// return CastCode.BESTCAST;
// }
//
// @Override
// public CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
// return new CodeMap(CastCode.BESTCAST | CodeMap.LazyFormat, "anycast",
// "anycast", this, toTy);
// }
//
// @Override
// public int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
// return CastCode.BESTCAST;
// }
//
// @Override
// public CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
// return new CodeMap(CastCode.BESTCAST | CodeMap.LazyFormat, "upcast",
// "upcast", fromTy, this);
// }
//
// }