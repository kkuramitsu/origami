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

package origami.rule.iroha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import origami.ODebug;
import origami.OEnv;
import origami.asm.OAnno;
import origami.asm.OCallSite;
import origami.asm.code.DupCode;
import origami.code.GenerativeCode;
import origami.code.OGetterCode;
import origami.code.HookAfterCode;
import origami.code.MutableCode;
import origami.code.OArrayCode;
import origami.code.OCode;
import origami.code.OConstructorCode;
import origami.code.OEmptyCode;
import origami.code.OErrorCode;
import origami.code.OLabelBlockCode;
import origami.code.OLabelBlockCode.OBreakLabel;
import origami.code.OLabelBlockCode.OContinueLabel;
import origami.code.OSugarCode;
import origami.code.OWarningCode;
import origami.code.OWhileCode;
import origami.code.RunnableCode;
import origami.code.ThisCode;
import origami.ffi.OMutable;
import origami.ffi.OrigamiObject;
import origami.ffi.OrigamiPrimitiveGenerics;
import origami.lang.OClassDecl;
import origami.lang.OClassDeclType;
import origami.lang.OField;
import origami.lang.OLocalVariable;
import origami.lang.OMethod;
import origami.lang.OMethodHandle;
import origami.lang.OPartialFunc;
import origami.lang.OTypeName;
import origami.lang.callsite.OFuncCallSite;
import origami.nez.ast.SourcePosition;
import origami.nez.ast.Symbol;
import origami.nez.ast.Tree;
import origami.rule.AbstractTypeRule;
import origami.rule.OFmt;
import origami.rule.OSymbols;
import origami.rule.OrigamiIterator;
import origami.rule.SyntaxAnalysis;
import origami.type.AnyType;
import origami.type.OParamType;
import origami.type.OType;
import origami.type.OTypeSystem;
import origami.type.OUntypedType;
import origami.util.OImportable;
import origami.util.OTypeRule;
import origami.util.OTypeUtils;

public class IrohaRules implements OImportable, OSymbols, SyntaxAnalysis {

	public OTypeRule ExportDecl = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			for (OEnv cur = env.getParent(); cur != null; cur = cur.getParent()) {
				if (cur.getEntryPoint() != null) {
					setDefiningEnv(env, cur);
				}
			}
			return typeExpr(env, t.get(_body));
		}
	};

	public OTypeRule AssumeDecl = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> a) {
			OEnv defineEnv = getDefiningEnv(env);
			for (Tree<?> t : a.get(_body)) {
				OType type = parseType(env, t.get(_type));
				String[] names = parseNames(env, t.get(_name));
				for (String name : names) {
					defineEnv.add(t, name, OTypeName.newEntry(type));
				}
			}
			return new OEmptyCode(env);
		}
	};

	// assert(expr)

	public OTypeRule AssertExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String msg = ODebug.assertMessage(env, t.get(_cond));
			OMethodHandle assertFunc = new OMethod(env, ODebug.AssertMethod);
			OCode expr = typeCondition(env, t.get(_cond));
			return assertFunc.newMethodCode(env, expr, env.v(msg));
		}
	};

	public OTypeRule AssertEqExpr = new InlineAssertRule("==");
	public OTypeRule AssertNeExpr = new InlineAssertRule("!=");
	public OTypeRule AssertLtExpr = new InlineAssertRule("<");
	public OTypeRule AssertLteExpr = new InlineAssertRule("<=");
	public OTypeRule AssertGtExpr = new InlineAssertRule(">");
	public OTypeRule AssertGteExpr = new InlineAssertRule(">=");

	public class InlineAssertRule extends AbstractTypeRule {
		final String op;

		public InlineAssertRule(String op) {
			this.op = op;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = typeExpr(env, t.get(_left));
			OCode right = typeExpr(env, t.get(_right));
			OCode cond = new DupCode(left).newBinaryCode(env, op, right);
			String msg = ODebug.assertMessage(env, t.get(_right));
			OMethodHandle assertFunc = new OMethod(env, ODebug.AssertMethod);
			return new HookAfterCode(left, assertFunc.newMethodCode(env, cond, env.v(msg)));
		}
	}

	public OTypeRule MutableExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode expr = typeExpr(env, t.get(_expr));
			OType ty = expr.getType();
			if (ty.isPrimitive()) {
				return new OWarningCode(expr, OFmt.YY0_is_meaningless, OFmt.quote("new")).setSourcePosition(t);
			}
			if (!(expr instanceof OConstructorCode)) {
				if (ty.isA(Cloneable.class)) {
					expr = expr.newMethodCode(env, "clone");
					expr = expr.refineType(env, ty);
				} else {
					return new OErrorCode(env, t, OFmt.not_clonable);
				}
			}
			if (ty.isOrigami()) {
				return new MutableCode(expr);
			}
			return expr;
		}
	};

	public OTypeRule TweetExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode expr = typeExpr(env, t.get(_expr));
			OCode file = env.v(t.getSource().getResourceName());
			OCode linenum = env.v(t.getSource().linenum(t.getSourcePosition()));
			OCode code = env.v(t.get(_expr).toText());
			OCode type = env.v(expr.getType().toString());
			return OCallSite.findParamCode(env, OFuncCallSite.class, "p", expr, file, linenum, code, type);
		}
	};

	public OTypeRule EnvExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			Class<?> c = env.findEntryPoint();
			return new OGetterCode(new OField(env, OTypeUtils.loadField(c, "entry")));
		}
	};

	public OTypeRule EmptyExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType ty = null;
			if (t.has(_type)) {
				ty = parseType(env, t.get(_type));
			} else {
				ty = typeExpr(env, t.get(_expr)).getType();
			}
			return new OEmptyCode(ty);
		}
	};

	public class ArrayRule extends AbstractTypeRule {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType ctype = env.t(OUntypedType.class);
			ArrayList<OCode> l = new ArrayList<>(t.size());
			for (int i = 0; i < t.size(); i++) {
				OCode element = typeExpr(env, t.get(i));
				if (ctype.isUntyped()) {
					ctype = element.getType();
				}
				if (!(element instanceof OEmptyCode)) {
					l.add(element);
				}
			}
			if (!ctype.isUntyped()) {
				for (int i = 0; i < l.size(); i++) {
					l.set(i, typeCheck(env, ctype, l.get(i)));
				}
			}
			return newListCode(env, ctype, l.toArray(new OCode[l.size()]));
		}

		protected OCode newListCode(OEnv env, OType ctype, OCode[] nodes) {
			return new OArrayCode(ctype, nodes);
		}
	}

	public class ListRule extends ArrayRule {
		final boolean isMutable;

		ListRule(boolean isMutable) {
			this.isMutable = isMutable;
		}

		@Override
		protected OCode newListCode(OEnv env, OType ctype, OCode[] nodes) {
			OCode listCode = new ListCode(env, OrigamiList.newListType(ctype), new OArrayCode(ctype, nodes));
			if (this.isMutable) {
				return new MutableCode(listCode);
			}
			return listCode;
		}
	}

	static class ListCode extends OSugarCode {
		OArrayCode arrayCode;

		ListCode(OEnv env, OType ret, OArrayCode a) {
			super(env, ret);
			this.arrayCode = a;
		}

		@Override
		public OCode desugar() {
			return this.getType().newConstructorCode(env(), this.arrayCode);
		}
	}

	public OTypeRule ArrayExpr = new ArrayRule();
	public OTypeRule ListExpr = new ListRule(false);
	public OTypeRule MutListExpr = new ListRule(true);
	// public OTypeRule MutListExpr = new MutListRule();

	/* Map */

	public OTypeRule DictExpr = new DictRule(false);
	public OTypeRule MutDictExpr = new DictRule(true);

	public class DictRule extends AbstractTypeRule {
		final boolean isMutable;

		DictRule(boolean isMutable) {
			this.isMutable = isMutable;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType ktype = env.t(String.class);
			OType vtype = env.t(OUntypedType.class);
			ArrayList<OCode> keys = new ArrayList<>(t.size());
			ArrayList<OCode> values = new ArrayList<>(t.size());
			HashSet<String> duplicatedChecker = new HashSet<>();
			for (int i = 0; i < t.size(); i++) {
				Tree<?> e = t.get(i);
				String key = e.getText(_name, "#");
				OCode element = typeExpr(env, e.get(_value));
				if (vtype.isUntyped()) {
					vtype = element.getType();
				}
				if (!(element instanceof OEmptyCode)) {
					if (duplicatedChecker.contains(key)) {
						element = new OWarningCode(element, OFmt.YY0_is_duplicated, key);
					}
					keys.add(env.v(key));
					values.add(element);
					duplicatedChecker.add(key);
				}
			}
			if (vtype.isPrimitive()) {
				vtype = vtype.boxType();
			}
			if (vtype.isUntyped()) {
				vtype = env.t(Object.class);
			} else {
				for (int i = 1; i < keys.size(); i += 2) {
					keys.set(i, typeCheck(env, vtype, keys.get(i)));
				}
			}
			OCode key = new OArrayCode(ktype, keys.toArray(new OCode[keys.size()]));
			OCode value = new OArrayCode(vtype, values.toArray(new OCode[keys.size()]));
			OType mapType = OParamType.of(env.t(OrigamiDictMap.class), vtype);
			OCode mapCode = mapType.newConstructorCode(env, key, value);
			if (this.isMutable) {
				return new MutableCode(mapCode);
			}
			return mapCode;
		}
	}

	public static class OrigamiDictMap<T> extends HashMap<String, T> implements OrigamiObject {
		private static final long serialVersionUID = 439376501967106812L;

		public OrigamiDictMap(String[] keys, T[] values) {
			for (int i = 0; i < keys.length; i++) {
				this.put(keys[i], values[i]);
			}
		}

		@OMutable
		public void set(String key, T value) {
			this.put(key, value);
		}

	}

	/* Tree */

	public OTypeRule TreeExpr = new TreeRule(false);
	public OTypeRule MutTreeExpr = new TreeRule(true);

	public class TreeRule extends AbstractTypeRule {
		final boolean isMutable;

		TreeRule(boolean isMutable) {
			this.isMutable = isMutable;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			ArrayList<OCode> keys = new ArrayList<>(t.size());
			ArrayList<OCode> values = new ArrayList<>(t.size());
			HashSet<String> duplicatedChecker = new HashSet<>();
			for (int i = 0; i < t.size(); i++) {
				Tree<?> e = t.get(i);
				String key = e.getText(_name, "#");
				OCode element = typeExpr(env, e.get(_value));
				if (e.has(_name) && "NameExpr".equals(e.get(_name).getTag().getSymbol())) {
					OType ty = OTypeName.getType(env, key);
					if (ty != null) {
						element = element.asType(env, ty);
					} else {
						element = new OWarningCode(element, OFmt.YY0_is_unknown_name, key);
					}
				}
				keys.add(env.v(Symbol.unique(key).id()));
				if (duplicatedChecker.contains(key)) {
					element = new OWarningCode(element, OFmt.YY0_is_duplicated, key);
				}
				values.add(element);
				duplicatedChecker.add(key);
			}
			OCode key = new OArrayCode(env.t(int.class), keys.toArray(new OCode[keys.size()]));
			OCode value = new OArrayCode(env.t(Object.class), values.toArray(new OCode[keys.size()]));
			OCode treeCode = env.t(IObject.class).newConstructorCode(env, key, value);
			if (this.isMutable) {
				return new MutableCode(treeCode);
			}
			return treeCode;

		}
	}

	// static OType IRangeType = TS.unique(IRange.class);

	public OTypeRule RangeUntilExpr = new RangeRule(false);
	public OTypeRule RangeExpr = new RangeRule(true);

	class RangeRule extends AbstractTypeRule {
		boolean inclusive;

		public RangeRule(boolean inclusive) {
			this.inclusive = inclusive;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = typeExpr(env, t.get(_left));
			OCode right = typeExpr(env, t.get(_right));
			OType rangeType = OParamType.of(env.t(IRange.class), left.getType());
			return rangeType.newConstructorCode(env, left, right, env.v(inclusive));
		}

	}

	/* mutable */

	public OTypeRule ForEachExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getText(_name, null);
			OCode iterCode = OrigamiIterator.newIteratorCode(env, ensureTypedExpr(env, t.get(_expr)));
			OCode nextCode = ForEachCode.nextCode(env, iterCode);
			OType nameType = nextCode.getType();
			// ODebug.trace("iter %s %s", nameType, iterCode);
			OEnv lenv = env.newEnv();
			lenv.add(name, new OLocalVariable(true, name, nameType));
			OCode bodyCode = typeExprOrErrorCode(lenv, t.get(_body));
			return new ForEachCode(lenv, name, nameType, iterCode, bodyCode);
		}
	};

	static class ForEachCode extends OSugarCode {
		final String name;
		final OType nextType;
		// final OCode nextCode;

		protected ForEachCode(OEnv env, String name, OType nextType, OCode iterCode, OCode bodyCode) {
			super(env, null, iterCode, bodyCode);
			this.name = name;
			this.nextType = nextType;
			// this.nextCode = nextCode;
		}

		private OCode iterCode() {
			return nodes[0];
		}

		private OCode bodyCode() {
			return nodes[1];
		}

		@Override
		public OType getType() {
			OType t = nodes[1].getType();
			if (t.is(void.class) || t.isUntyped()) {
				return t;
			}
			return t.getTypeSystem().newArrayType(t);
		}

		@Override
		public OCode refineType(OEnv env, OType t) {
			nodes[1] = nodes[1].refineType(this.env(), t);
			return this;
		}

		@Override
		public OCode desugar() {
			if (this.getType().is(void.class)) {
				return desugarStmtCode();
			}
			return desugarStmtCode();
		}

		public OCode desugarStmtCode() {
			GenerativeCode outer = new GenerativeCode(env(), null);
			outer.pushDefine("it", iterCode());
			//
			outer.pushDefine(name, nextType);
			OCode hasNext = outer._var("it").newMethodCode(env(), "hasNext");

			GenerativeCode loop = new GenerativeCode(env(), outer);
			// ODebug.trace("nextCode=%s", nextCode(outer.name("it")));

			loop.pushAssign(name, nextCode(env(), outer._var("it")));
			// loop.p(outer.name(name));
			loop.push(bodyCode());

			OLabelBlockCode block = new OLabelBlockCode(null, new OWhileCode(env(), hasNext, loop), loop._empty());
			env().add(OBreakLabel.class, new OBreakLabel(block));
			env().add(OContinueLabel.class, new OContinueLabel(block));
			outer.push(block);
			return outer;
		}

		public OCode desugarExprCode() {
			GenerativeCode outer = new GenerativeCode(env(), null);
			outer.pushDefine("it", iterCode());
			outer.pushDefine(name, nextType);

			OType listType = OrigamiList.newListType(nextType);
			outer.pushDefine("a", listType.newConstructorCode(env()));
			OCode hasNext = outer._var("it").newMethodCode(env(), "hasNext");

			GenerativeCode loop = new GenerativeCode(env(), outer);
			loop.pushAssign(name, nextCode(env(), outer._var("it")));
			loop.push(outer._var("a").newMethodCode(env(), "add", bodyCode()).asType(env(), void.class));

			OLabelBlockCode block = new OLabelBlockCode(null, new OWhileCode(env(), hasNext, loop), loop._var("a"));
			env().add(OBreakLabel.class, new ForEachBreak(outer._var("a"), block));
			env().add(OContinueLabel.class, new ForEachContinue(outer._var("a"), block));
			outer.push(block);
			outer.push(outer._var("a").asType(env(), this.getType()));
			return outer;
		}

		// private static OType nextType(OCode iterCode) {
		// OType targetType = iterCode.getType();
		// return targetType.getParamTypes()[0].unboxType();
		// }

		private static OCode nextCode(OEnv env, OCode base) {
			// ODebug.trace("primitive iterator %s %s", base.getType(), base);
			if (base.getType().isA(OrigamiPrimitiveGenerics.class)) {
				// ODebug.trace("primitive iterator %s", base);
				return base.newMethodCode(env, "nextp");
			}
			return base.newMethodCode(env, "next");
		}

		class ForEachBreak extends OLabelBlockCode.OBreakLabel {
			OCode list;

			public ForEachBreak(OCode list, OLabelBlockCode block) {
				super(block);
				this.list = list;
			}

			@Override
			public OCode newHookCode(OEnv env, OCode expr) {
				OType t = this.block.getType().getParamTypes()[0];
				return list.newMethodCode(env, "add", expr.asType(env, t)).asType(env, env.t(void.class));
			}

		}

		class ForEachContinue extends OLabelBlockCode.OContinueLabel {
			OCode list;

			public ForEachContinue(OCode list, OLabelBlockCode block) {
				super(block);
				this.list = list;
			}

			@Override
			public OCode newHookCode(OEnv env, OCode expr) {
				OType t = this.block.getType().getParamTypes()[0];
				return list.newMethodCode(env, "add", expr.asType(env, t)).asType(env, env.t(void.class));
			}

		}
	}

	public OTypeRule ClassDecl = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			/* annotations */
			OAnno anno = parseAnno(env, "public", t.get(_anno, null));
			String name = t.getText(_name, null);

			/* extends */
			OType superType = env.t(Object.class);
			if (t.has(_super)) {
				superType = parseType(env, t.get(_super), superType);
			}

			/* implements */
			OType[] interfaces = parseInterfaceTypes(env, t.get(_impl, null));

			OType[] params = null;
			OClassDeclType ct = new OClassDeclType(env, anno, name, null, superType, interfaces);
			ct.getDecl().addBody(t.get(_body, null));
			defineName(env, t, ct);

			if (t.has(_param)) {
				OTypeSystem ts = env.getTypeSystem();
				String[] paramNames = parseParamNames(env, t.get(_param, null));
				OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), env.t(AnyType.class));
				for (int i = 0; i < paramNames.length; i++) {
					ct.addField(anno, paramTypes[i], paramNames[i], null);
				}
				OMethodHandle constr = null;
				for (OMethodHandle c : superType.getConstructors()) {
					if (constr == null || constr.getParamSize() < c.getParamSize()) {
						constr = c;
					}
				}
				// ConstructDecl c = new ConstructorDecl();
				// OMethod.newMethod(cdecl.getType(), anno, paramNames,
				// paramTypes, exceptions);
			}
			return new RunnableCode(env, ct.getDecl()::typeCheck);
		}
	};

	public OTypeRule MethodDecl = new AbstractTypeRule() {

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl cdecl = checkClassContext(env);
			OAnno anno = parseAnno(env, "public", t.get(_anno, null));
			OType rtype = parseType(env, t.get(_type, null), env.t(OUntypedType.class));

			String name = t.getText(_name, "");
			String[] paramNames = parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), env.t(AnyType.class));
			OType[] exceptions = parseExceptionTypes(env, t.get(_throws, null));

			OCode body = parseUntypedCode(env, t.get(_body, null));
			OMethodHandle m = cdecl.addMethod(anno, rtype, name, paramNames, paramTypes, exceptions, body);
			if (anno.isStatic()) {
				defineName(env, t, m);
			} else {
				defineName(env, t, new OPartialFunc(m, 0, new ThisCode(cdecl.getType())));
			}
			return new OEmptyCode(env);
		}

	};

	static class OrigamiEnum {
		String name;
		int id;
	}

	// public OTypeRule EnumDecl = new AbstractTypeRule() {
	// @Override
	// public OCode typeRule(OEnv env, Tree<?> t) {
	// try {
	// System.out.println(t);
	// /* annotations */
	// OAnno anno = parseAnno(env, "public", t.get(_anno, null));
	// String name = t.getText(_name, null);
	// OEnv cenv = env.newEnv();
	// ClassDecl cdecl = new ClassDecl(cenv, anno, name, null,
	// env.t(Object.class)/*OType.Object*/);
	// env.getClassLoader().addClassDecl(cdecl);
	// Class<?> c = env.getClassLoader().getCompiledClass(cdecl.getName());
	// defineName(env, t, new ClassDefined(c));
	// Tree<?> param = t.get(_param);
	// Constructor<?> cc = TypeUtils.loadConstructor(c, int.class,
	// String.class);
	// for (int i = 0; i < t.size(); i++) {
	// // Object o = cc.newInstance(i, t.get(i).toText());
	// }
	// return new EmptyCode();
	// } catch (ErrorCodeException e) {
	// return e.newErrorCode();
	// }
	// }
	// };

}
