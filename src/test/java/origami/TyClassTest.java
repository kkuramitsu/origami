// package origami;
//
// import java.util.function.Function;
//
// import blue.origami.common.OConsole;
// import blue.origami.transpiler.type.Ty;
//
// public class TyClassTest {
//
// public void testMatchIntVar() {
// Ty left = Ty.tVar(null);
// assert left.match(Ty.tInt);
// assert left.match(Ty.tInt);
// assert !left.match(Ty.tBool);
// this.check(left, Ty.tInt);
// }
//
// public void testMatchVarInt() {
// Ty right = Ty.tVar(null);
// assert Ty.tInt.match(right);
// assert Ty.tInt.match(right);
// assert !Ty.tBool.match(right);
// this.check(right, Ty.tInt);
// }
//
// public void testMatchVarVar() {
// Ty left = Ty.tVar(null);
// Ty right = Ty.tVar(null);
// assert left.match(right);
// assert left.match(Ty.tInt);
// assert right.match(Ty.tInt);
// assert Ty.tInt.match(left);
// assert Ty.tInt.match(right);
// this.check(left, Ty.tInt);
// this.check(right, Ty.tInt);
// }
//
// public void testMatchListVar() {
// Ty ty = Ty.tList(Ty.tInt);
// Ty ty2 = Ty.tList(Ty.tVar(null));
// assert ty.match(ty2);
// assert ty2.match(ty);
// this.check(ty2, "List[Int]");
// }
//
// public void testMatchMutInt() {
// Ty ty = Ty.tInt;
// Ty mut = ty.toMutable();
// this.check(mut, "List[Int]");
// assert ty.match(mut);
// assert !mut.match(ty);
// }
//
// public void testMatchMutListInt() {
// Ty ty = Ty.tList(Ty.tInt);
// Ty mut = ty.toMutable();
// this.check(mut, "List[Int]");
// assert ty.match(mut);
// assert !mut.match(ty);
// }
//
// // public void testMutVar() {
// // Ty ty = Ty.tList(Ty.tInt);
// // Ty var = Ty.tVar();
// // Ty mut = var.toMutable();
// // assert mut.match(ty);
// // }
//
// // public void testEq() {
// // assert Ty.tBool.eq(Ty.tBool) : "Bool == Bool";
// // assert Ty.tInt.eq(Ty.tInt) : "Int == Int";
// // assert !Ty.tBool.eq(Ty.tInt) : "Bool != Bool";
// // }
// //
// // public void testMemo() {
// // Function<Ty, Object> f = (ty) -> ty.memoed();
// // this.check(Ty.tBool, "memo", f, "Bool");
// // this.check(Ty.tVarParam[1], "memo", f, "b");
// // this.check(Ty.tList(Ty.tInt), "memo", f, "List[Int]");
// // this.check(Ty.tTag(Ty.tString, "JSON"), "memo", f, "String #JSON");
// // // this.check(Ty.tCond(Ty.tVarParam[0], false, Ty.tInt), "memo", f, "a
// // !Int");
// //
// // }
// //
// // public void testKeyFrom() {
// // Function<Ty, Object> f = (ty) -> ty.keyOfArrows();
// // this.check(Ty.tBool, "keyFrom", f, "Bool");
// // this.check(Ty.tVarParam[1], "keyFrom", f, "a");
// // this.check(Ty.tList(Ty.tInt), "keyFrom", f, "List");
// // this.check(Ty.tList(Ty.tVarParam[0]), "keyFrom", f, "List");
// // this.check(Ty.tTag(Ty.tString, "JSON"), "keyFrom", f, "String");
// // }
//
// public void testData() {
// Ty t0 = Ty.tData();
// Ty t1 = Ty.tData("x", "y");
// Ty t2 = Ty.tData("x", "y", "z");
// Ty t3 = Ty.tData("x", "z");
// assert t0.match(t1);
// assert t0.match(t2);
// assert t0.match(t3);
// assert t1.match(t2);
// assert !t1.match(t3);
// assert !t0.eq(t1);
// assert !t0.eq(t2);
// assert !t0.eq(t3);
// assert !t1.eq(t2);
// assert !t1.eq(t3);
// assert !t1.match(t0);
// assert !t2.match(t0);
// assert !t3.match(t0);
// assert !t2.match(t1);
// assert !t3.match(t1);
// }
//
// public void testDataVar() {
// Ty var = Ty.tVar(null);
// }
//
// public void testDataLeftVar() {
// Ty t1 = Ty.tData("x", "y");
// Ty var = Ty.tVar(null);
// // var = t;
// assert var.match(t1);
// this.check(var, "{x,y}");
// assert var.eq(t1);
// }
//
// public void testDataRightVar() {
// Ty t1 = Ty.tData("x", "y");
// Ty var = Ty.tVar(null);
// // t1 = t;
// assert t1.match(var);
// this.check(var, "{x,y}");
// assert var.eq(t1);
// }
//
// public void testTag() {
// Ty t0 = Ty.tInt;
// Ty t1 = Ty.tTag(Ty.tInt, "x", "y");
// Ty t2 = Ty.tTag(Ty.tInt, "x", "y", "z");
// Ty t3 = Ty.tTag(Ty.tInt, "x", "z");
//
// assert t0.match(t1);
// assert t0.match(t2);
// assert t0.match(t3);
// assert t1.match(t2);
// assert !t1.match(t3);
//
// assert !t0.eq(t1);
// assert !t0.eq(t2);
// assert !t0.eq(t3);
// assert !t1.eq(t2);
// assert !t1.eq(t3);
//
// assert !t1.match(t0);
// assert !t2.match(t0);
// assert !t3.match(t0);
// assert !t2.match(t1);
// assert !t3.match(t1);
// }
//
// // public void testCondType() {
// // Ty t0 = Ty.tList(Ty.tUntyped());
// // Ty t1 = Ty.tList(Ty.tCond(Ty.tUntyped(), false, Ty.tInt));
// // Ty t2 = Ty.tList(Ty.tCond(Ty.tCond(Ty.tUntyped(), false, Ty.tInt), false,
// // Ty.tFloat));
// // System.out.println(":: t1=" + t1);
// // System.out.println(":: t2=" + t2);
// // assert t0.match(Ty.tList(Ty.tInt));
// // assert !t1.match(Ty.tList(Ty.tInt));
// // System.out.println(":: t1=" + t1);
// // assert t1.match(Ty.tList(Ty.tFloat));
// // System.out.println(":: t1=" + t1);
// // assert !t2.match(Ty.tList(Ty.tInt));
// // assert !t2.match(Ty.tList(Ty.tFloat));
// // System.out.println(":: t2=" + t2);
// // assert t2.match(Ty.tList(Ty.tString));
// // System.out.println(":: t2=" + t2);
// // }
//
// // public void testTagType() {
// // Ty t0 = Ty.tString;
// // Ty t1 = Ty.tTag(Ty.tString, "a");
// // Ty t2 = Ty.tTag(Ty.tString, "a", "b");
// // Ty t3 = Ty.tTag(Ty.tString, "a", "b", "c");
// // Ty t4 = Ty.tTag(Ty.tString, "a", "c");
// // // assert t0.match(t1);
// // // assert t0.match(t2);
// // // assert t0.match(t3);
// // // assert t0.match(t4);
// // assert t1.match(t2);
// // assert t1.match(t3);
// // assert t1.match(t4);
// // assert t2.match(t3);
// // assert !t2.match(t4);
// // assert !t3.match(t0);
// // assert !t3.match(t1);
// // assert !t3.match(t2);
// // assert !t3.match(t4);
// // ;
// // ;
// // assert !t0.eq(t1);
// // assert !t0.eq(t2);
// // assert !t0.eq(t3);
// // assert !t0.eq(t4);
// // assert !t1.eq(t2);
// // assert !t1.eq(t3);
// // assert !t1.eq(t4);
// // assert !t2.eq(t3);
// // assert !t2.eq(t4);
// // assert !t3.eq(t0);
// // assert !t3.eq(t1);
// // assert !t3.eq(t2);
// // assert !t3.eq(t4);
// // }
//
// public void check(Object o, Object o2) {
// String t = o.toString();
// String ok = o2.toString();
// if (!t.endsWith(ok)) {
// OConsole.println("" + t + "" + OConsole.color(OConsole.Red, " != " + ok));
// }
// }
//
// public void check(Ty ty, String method, Function<Ty, Object> f, String ok) {
// Object res = f.apply(ty);
// if (ok != null && ok.equals(res.toString())) {
// OConsole.println("(" + ty + ")." + method + "=> " + res);
// } else {
// OConsole.println("(" + ty + ")." + method + "=> " + res +
// OConsole.color(OConsole.Red, " != " + ok));
// }
// }
//
// }
