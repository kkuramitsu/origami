package origami;

import java.util.function.Function;

import blue.origami.common.OConsole;
import blue.origami.transpiler.type.Ty;

public class TyTest {

	public void testEq() {
		assert Ty.tBool.eq(Ty.tBool) : "Bool == Bool";
		assert Ty.tInt.eq(Ty.tInt) : "Int == Int";
		assert !Ty.tBool.eq(Ty.tInt) : "Bool != Bool";
	}

	public void testType() {
		Function<Ty, Object> f = (ty) -> ty.memoed();
		this.check(Ty.tBool, "memo", f, "Bool");
		this.check(Ty.tVarParam[1], "memo", f, "b");
		this.check(Ty.tList(Ty.tInt), "memo", f, "List[Int]");
		this.check(Ty.tTag(Ty.tString, "JSON"), "memo", f, "String #JSON");
		this.check(Ty.tCond(Ty.tVarParam[0], false, Ty.tInt), "memo", f, "a !Int");

	}

	public void testKeyFrom() {
		Function<Ty, Object> f = (ty) -> ty.keyFrom();
		this.check(Ty.tBool, "keyFrom", f, "Bool");
		this.check(Ty.tVarParam[1], "keyFrom", f, "a");
		this.check(Ty.tList(Ty.tInt), "keyFrom", f, "List");
		this.check(Ty.tList(Ty.tVarParam[0]), "keyFrom", f, "List");
		this.check(Ty.tTag(Ty.tString, "JSON"), "keyFrom", f, "String");
	}

	public void testDataType() {
		Ty t0 = Ty.tRecord();
		Ty t1 = Ty.tRecord("x", "y");
		Ty t2 = Ty.tRecord("x", "y", "z");
		Ty t3 = Ty.tRecord("x", "z");
		assert t0.match(t1);
		assert t0.match(t2);
		assert t0.match(t3);
		assert t1.match(t2);
		assert !t1.match(t3);
		assert !t0.eq(t1);
		assert !t0.eq(t2);
		assert !t0.eq(t3);
		assert !t1.eq(t2);
		assert !t1.eq(t3);
		assert !t1.match(t0);
		assert !t2.match(t0);
		assert !t3.match(t0);
		assert !t2.match(t1);
		assert !t3.match(t1);
	}

	public void testCondType() {
		Ty t0 = Ty.tList(Ty.tUntyped());
		Ty t1 = Ty.tList(Ty.tCond(Ty.tUntyped(), false, Ty.tInt));
		Ty t2 = Ty.tList(Ty.tCond(Ty.tCond(Ty.tUntyped(), false, Ty.tInt), false, Ty.tFloat));
		System.out.println(":: t1=" + t1);
		System.out.println(":: t2=" + t2);
		assert t0.match(Ty.tList(Ty.tInt));
		assert !t1.match(Ty.tList(Ty.tInt));
		System.out.println(":: t1=" + t1);
		assert t1.match(Ty.tList(Ty.tFloat));
		System.out.println(":: t1=" + t1);
		assert !t2.match(Ty.tList(Ty.tInt));
		assert !t2.match(Ty.tList(Ty.tFloat));
		System.out.println(":: t2=" + t2);
		assert t2.match(Ty.tList(Ty.tString));
		System.out.println(":: t2=" + t2);
	}

	// public void testTagType() {
	// Ty t0 = Ty.tString;
	// Ty t1 = Ty.tTag(Ty.tString, "a");
	// Ty t2 = Ty.tTag(Ty.tString, "a", "b");
	// Ty t3 = Ty.tTag(Ty.tString, "a", "b", "c");
	// Ty t4 = Ty.tTag(Ty.tString, "a", "c");
	// // assert t0.match(t1);
	// // assert t0.match(t2);
	// // assert t0.match(t3);
	// // assert t0.match(t4);
	// assert t1.match(t2);
	// assert t1.match(t3);
	// assert t1.match(t4);
	// assert t2.match(t3);
	// assert !t2.match(t4);
	// assert !t3.match(t0);
	// assert !t3.match(t1);
	// assert !t3.match(t2);
	// assert !t3.match(t4);
	// ;
	// ;
	// assert !t0.eq(t1);
	// assert !t0.eq(t2);
	// assert !t0.eq(t3);
	// assert !t0.eq(t4);
	// assert !t1.eq(t2);
	// assert !t1.eq(t3);
	// assert !t1.eq(t4);
	// assert !t2.eq(t3);
	// assert !t2.eq(t4);
	// assert !t3.eq(t0);
	// assert !t3.eq(t1);
	// assert !t3.eq(t2);
	// assert !t3.eq(t4);
	// }

	public void check(Ty ty, String method, Function<Ty, Object> f, String ok) {
		Object res = f.apply(ty);
		if (ok != null && ok.equals(res.toString())) {
			OConsole.println("(" + ty + ")." + method + "=> " + res);
		} else {
			OConsole.println("(" + ty + ")." + method + "=> " + res + OConsole.color(OConsole.Red, " != " + ok));
		}
	}

}
