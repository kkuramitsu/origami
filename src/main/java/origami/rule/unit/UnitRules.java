/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package origami.rule.unit;

import java.lang.reflect.Constructor;

import origami.asm.OAnno;
import origami.asm.OClassLoader;
import origami.asm.code.LoadArgCode;
import origami.asm.code.LoadThisCode;
import origami.code.OClassInitCode;
import origami.code.OCode;
import origami.code.OEmptyCode;
import origami.code.OErrorCode;
import origami.code.OReturnCode;
import origami.code.OTypeCode;
import origami.code.OWarningCode;
import origami.ffi.OAlias;
import origami.ffi.OCast;
import origami.ffi.OImportable;
import origami.lang.OClassDeclType;
import origami.lang.OEnv;
import origami.lang.OTypeName;
import origami.nez.ast.Symbol;
import origami.nez.ast.Tree;
import origami.rule.TypeRule;
import origami.rule.OFmt;
import origami.rule.OSymbols;
import origami.rule.SyntaxAnalysis;
import origami.type.OLocalClassType;
import origami.type.OParamType;
import origami.type.OType;
import origami.type.OUntypedType;
import origami.type.ThisType;
import origami.util.ODebug;
import origami.util.OScriptUtils;
import origami.util.OTypeRule;
import origami.util.OTypeUtils;

public class UnitRules implements OImportable, OSymbols, SyntaxAnalysis, OScriptUtils {
	public OTypeRule UnitType = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = "[" + t.toText() + "]";
			OType ty = OTypeName.getType(env, name);
			if (ty == null) {
				throw new OErrorCode(env, t, OFmt.undefined_unit__YY0, name);
			}
			return new OTypeCode(ty);
		}
	};

	public OTypeRule UnitLiteral = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			try {
				double d = Double.valueOf(t.getText(_value, "0"));
				try {
					OType ty = parseType(env, t.get(_type));
					return ty.newConstructorCode(env, env.v(d));
				} catch (OErrorCode e) {
					return new OWarningCode(env.v(d), OFmt.undefined_unit__YY0, t.get(_type).toText());
				}
			} catch (NumberFormatException e) {
				throw new OErrorCode(env, t.get(_value), OFmt.syntax_error);
			}
		}

	};

	public final static Symbol _shift = Symbol.unique("shift");
	public final static Symbol _ishift = Symbol.unique("ishift");
	public final static Symbol _scale = Symbol.unique("scale");
	public final static Symbol _iscale = Symbol.unique("iscale");

	public OTypeRule UnitDecl = new TypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getText(_name, null);
			OType baseUnit = parseType(env, t.get(_base));
			double shift = 0.0;
			if (t.has(_shift)) {
				shift = Double.valueOf(t.getText(_shift, "0"));
			} else if (t.has(_ishift)) {
				shift = -Double.valueOf(t.getText(_ishift, "0"));
			}
			double scale = 1.0;
			boolean inv = false;
			OType otherUnit = null;
			if (t.has(_scale)) {
				Tree<?> sc = t.get(_scale);
				if (sc.getTag().toString().equals("UnitType")) {
					otherUnit = parseType(env, sc);
				} else {
					scale = Double.valueOf(sc.toText());
				}
			}
			if (t.has(_iscale)) {
				Tree<?> sc = t.get(_iscale);
				inv = true;
				if (sc.getTag().toString().equals("UnitType")) {
					otherUnit = parseType(env, sc);
				} else {
					scale = 1.0 / Double.valueOf(sc.toText());
				}
			}
			OClassDeclType targetUnit = createUnitType(env, name);
			if (otherUnit == null) { // conversion
				// target = base * scale + shift
				addUnitConv(env, targetUnit, targetUnit, baseUnit, scale, shift);
				// base = (target - shift) / scale
				addUnitConv(env, targetUnit, baseUnit, targetUnit, 1.0 / scale, -shift / scale);
			} else { // operators
				if (inv) {
					// t = b / o
					defineDiv(env, targetUnit, targetUnit, baseUnit, otherUnit);
					// o = b / t
					defineDiv(env, targetUnit, otherUnit, baseUnit, targetUnit);
					// b = t * o
					defineMul(env, targetUnit, baseUnit, targetUnit, otherUnit);
					defineMul(env, targetUnit, baseUnit, otherUnit, targetUnit);
				} else {
					if (otherUnit.eq(baseUnit)) {
						// t = b * o
						defineMul(env, targetUnit, targetUnit, otherUnit, baseUnit);
						// b = t / o
						defineDiv(env, targetUnit, baseUnit, targetUnit, otherUnit);
					} else {
						// t = b * o, t = o * b
						defineMul(env, targetUnit, targetUnit, otherUnit, baseUnit);
						defineMul(env, targetUnit, targetUnit, baseUnit, otherUnit);
						// b = t / o
						defineDiv(env, targetUnit, baseUnit, targetUnit, otherUnit);
						// o = t / b
						defineDiv(env, targetUnit, otherUnit, targetUnit, baseUnit);
					}
				}
			}
			String unitName = "[" + name + "]";
			Class<?> c = targetUnit.unwrap(env);
			ODebug.trace("compiled %s", c);
			importClassMethod(env, null, c);
			OType unitType = new OLocalClassType(env, c, unitName, null);
			env.getTypeSystem().define(c, unitType);
			env.add(unitName, unitType);
			return new OEmptyCode(env);
		}

	};

	private int serialNumber = 0;

	private OClassDeclType createUnitType(OEnv env, String name) {
		String cname = "UnitType$" + serialNumber++;
		ThisType thisType = new ThisType(env.t(OUntypedType.class));
		OType superClass = OParamType.of(OUnit.class, thisType);
		OClassLoader cl = env.getClassLoader();
		OClassDeclType ct = cl.newType(env, new OAnno("public"), cname, OType.emptyTypes, superClass);
		thisType.setType(ct);
		addUnitMethods(env, ct, name);
		return ct;
	}

	Constructor<?> unitConstructor1 = OTypeUtils.loadConstructor(OUnit.class, double.class);
	Constructor<?> unitConstructor0 = OTypeUtils.loadConstructor(OUnit.class);

	public void addUnitMethods(OEnv env, OClassDeclType ct, String unit) {
		// Unit(double value) { super(value); }
		String[] paramNames = { "value" };
		OType[] paramTypes = { env.t(double.class) };
		OCode init1 = new OClassInitCode(env, unitConstructor1, new LoadThisCode(ct),
				new LoadArgCode(0, paramTypes[0]));
		ct.addConstructor(A("public"), paramNames, paramTypes, OType.emptyTypes, new OReturnCode(env, init1));
		// Unit() { super(); }
		OCode init0 = new OClassInitCode(env, unitConstructor0, new LoadThisCode(ct));
		ct.addConstructor(A("public"), emptyNames, OType.emptyTypes, OType.emptyTypes, new OReturnCode(env, init0));
		// Unit newValue(double d) { return new Unit(d); }
		OCode new1 = ct.newConstructorCode(env, new LoadArgCode(0, paramTypes[0]));
		ct.addMethod(A("public,final"), env.t(OUnit.class), "newValue", paramNames, paramTypes, OType.emptyTypes,
				new OReturnCode(env, new1));
		// String unit() { return unit; }
		ct.addMethod(A("public,final"), env.t(String.class), "unit", emptyNames, OType.emptyTypes, null,
				new OReturnCode(env, env.v(unit)));
	}

	private void addUnitConv(OEnv env, OClassDeclType ct, OType targetUnit, OType baseUnit, double scale,
			double shift) {
		OCode base = new LoadArgCode(0, baseUnit).newMethodCode(env, "doubleValue");
		base = base.newBinaryCode(env, "*", env.v(scale));
		// ODebug.trace("base type=%s", base.getType());
		if (shift != 0.0) {
			base = base.newBinaryCode(env, "+", env.v(shift));
		}
		// ODebug.trace("base * type=%s", base.getType());
		base = targetUnit.newConstructorCode(env, base);

		String[] paramNames = { "value" };
		OType[] paramTypes = { baseUnit };
		OAnno anno = A("public,static");
		anno.setAnnotation(OCast.class, "cost", OCast.BXSAME);
		ct.addMethod(anno, targetUnit, "conv", paramNames, paramTypes, null, new OReturnCode(env, base));
	}

	private void defineMul(OEnv env, OClassDeclType ct, OType targetUnit, OType baseUnit, OType otherUnit) {
		String[] paramNames = { "x", "y" };
		OType[] paramTypes = { baseUnit, otherUnit };
		OCode x = new LoadArgCode(0, baseUnit).newMethodCode(env, "doubleValue");
		OCode y = new LoadArgCode(1, otherUnit).newMethodCode(env, "doubleValue");
		OCode r = targetUnit.newConstructorCode(env, x.newBinaryCode(env, "*", y));
		OAnno anno = A("public,static");
		anno.setAnnotation(OAlias.class, "name", "*");
		ct.addMethod(anno, targetUnit, "_mul", paramNames, paramTypes, null, new OReturnCode(env, r));
	}

	private void defineDiv(OEnv env, OClassDeclType ct, OType targetUnit, OType baseUnit, OType otherUnit) {
		String[] paramNames = { "x", "y" };
		OType[] paramTypes = { baseUnit, otherUnit };
		OCode x = new LoadArgCode(0, baseUnit).newMethodCode(env, "doubleValue");
		OCode y = new LoadArgCode(1, otherUnit).newMethodCode(env, "doubleValue");
		OCode r = targetUnit.newConstructorCode(env, x.newBinaryCode(env, "/", y));
		OAnno anno = A("public,static");
		anno.setAnnotation(OAlias.class, "name", "/");
		ct.addMethod(anno, targetUnit, "_div", paramNames, paramTypes, null, new OReturnCode(env, r));
	}

}
