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

package blue.origami.rule.iroha;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import blue.nez.ast.Symbol;
import blue.nez.ast.Tree;
import blue.origami.ffi.Case;
import blue.origami.ffi.FieldExtractable;
import blue.origami.ffi.ListExtractable;
import blue.origami.ffi.OImportable;
import blue.origami.ffi.ObjectExtractable;
import blue.origami.ffi.OrigamiException;
import blue.origami.ffi.SequenceExtractable;
import blue.origami.lang.OEnv;
import blue.origami.lang.OLocalVariable;
import blue.origami.lang.type.NullableType;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;
import blue.origami.ocode.OCode;
import blue.origami.ocode.DefaultValueCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.IfCode;
import blue.origami.ocode.OLocalCode;
import blue.origami.ocode.SugarCode;
import blue.origami.rule.BlockGen;
import blue.origami.rule.OFmt;
import blue.origami.rule.TypeRule;
import blue.origami.rule.unit.OUnit;
import blue.origami.util.ODebug;
import blue.origami.util.OTypeRule;
import blue.origami.util.StringCombinator;

public class MatchRules implements OImportable {

	public OTypeRule MatchExpr = new MatchRule();

	static class MatchRule extends TypeRule {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			// OLocalVariable[] lvars = env.getLocalVariables();
			// OEnv lenv = env.newEnv();
			OCode matchCode = this.typeExpr(env, t.get(_expr));
			ICaseCode[] cases = this.parseCases(env, t.get(_body));
			OCode match = new IMatchCode(env, matchCode, cases);
			return match;
		}

		private ICaseCode[] parseCases(OEnv env, Tree<?> body) {
			ICaseCode[] cases = new ICaseCode[body.size()];
			for (int i = 0; i < cases.length; i++) {
				Tree<?> t = body.get(i);
				ICase iCase = this.parseCaseBody(env, t);
				OEnv lenv = env.newEnv();
				HashMap<String, OType> varMap = new HashMap<>();
				iCase.updateLocalVariable(null, varMap);
				for (Map.Entry<String, OType> v : varMap.entrySet()) {
					lenv.add(v.getKey(), new OLocalVariable(true, v.getKey(), v.getValue()));
				}
				OCode condCode = this.typeCondition(lenv, t.get(_cond, null));
				OCode bodyCode = this.typeExprOrErrorCode(lenv, t.get(_body));
				cases[i] = new ICaseCode(lenv, i, iCase, condCode, bodyCode);
				ODebug.trace("varMap=%s => %s", varMap, cases[i].getType());
			}
			return cases;
			// OType t = env.t(OUntypedType.class);
			// for (CaseCode c : this.cases) {
			// if (t.isUntyped()) {
			// t = c.getType();
			// }
			// }
			// this.asType(env, t);
		}

		private ICase parseCaseBody(OEnv env, Tree<?> tbody) {
			OTypeSystem ts = env.getTypeSystem();
			Tree<?> t = tbody.get(_expr);
			String name = t.getText(_name, null);
			String tag = t.getTag().getSymbol();
			switch (tag) {
			case "AnyCase": {
				// String name = t.getText(_name, null); // FIXME
				return new AnyCase();
			}
			case "ValueCase": {
				if (t.has(_list)) {
					Object[] v = new Object[t.size(_list, 0)];
					int i = 0;
					for (Tree<?> e : t.get(_list)) {
						v[i] = this.parseConstantValue(env, e);
						if (i > 0) {
							if (!v[i - 1].getClass().equals(v[i].getClass())) {
								throw new ErrorCode(env, e, OFmt.S_must_be_S, e.toString(), ts.valueType(v[0]));
							}
						}
						i++;
					}
					OType ty = ts.valueType(v[0]);
					return new ValuesCase(ty, name, v);
				}
				Object v = this.parseConstantValue(env, t.get(_value));
				OType ty = env.getTypeSystem().valueType(v);
				// ODebug.trace("t=%s, v=%s", t, v);
				return new ValueCase(ty, name, v);
			}
			case "RangeCase":
			case "RangeUntilCase": {
				Object start = this.parseConstantValue(env, t.get(_start));
				Object end = this.parseConstantValue(env, t.get(_end));
				OType ty = ts.valueType(start);
				if (!start.getClass().equals(end.getClass())) {
					throw new ErrorCode(env, t.get(_end), OFmt.S_must_be_S, end, ty);
				}
				if (start instanceof Number) {
					return new DRangeValue(ty, name, (Number) start, (Number) end, !tag.equals("RangeUntilCase"));
				}
				if (start instanceof OUnit<?>) {
					return new DRangeValue(ty, name, (OUnit<?>) start, (OUnit<?>) end, !tag.equals("RangeUntilCase"));
				}
				if (start instanceof String) {
					return new SRangeValue(ty, name, (String) start, (String) end, !tag.equals("RangeUntilCase"));
				}
				throw new ErrorCode(env, t, "illenal range values %s", t.toString());
			}
			case "TypeCase": {
				OType type = this.parseParamType(env, t, name, t.get(_type, null), null);
				ICase inner = this.parseCaseBody(env, t.get(_body, null));
				// ODebug.trace("name=%s, type=%s", name, type);
				return new TypeCase(type, name, inner);
			}
			case "ExtractCase": {
				ICase[] l = new ICase[t.size()];
				int i = 0;
				for (Tree<?> e : t) {
					l[i] = this.parseCaseBody(env, e);
					i++;
				}
				return new ExtractCase(env.getStartPoint(), l);
			}
			case "ListMoreCase":
			case "ListCase": {
				ICase[] l = new ICase[t.size()];
				int i = 0;
				for (Tree<?> e : t) {
					l[i] = this.parseCaseBody(env, e);
					i++;
				}
				return tag.equals("ListMoreCase") ? new ListMoreCase(l, t.size()) : new ListCase(l, t.size());
			}
			case "FieldCase": {
				ICase[] l = new ICase[t.size()];
				int i = 0;
				for (Tree<?> e : t) {
					l[i] = this.parseCaseBody(env, e);
					if (l[i].getName() == null) {
						throw new ErrorCode(env, e, "no field name %s", e.toText());
					}
					i++;
				}
				return new FieldCase(l);
			}
			default:
				throw new ErrorCode(env, t, OFmt.undefined_syntax__YY0, t.getTag().getSymbol());
			}
		}

	}

	public static class IMatchCode extends SugarCode {
		ICaseCode[] caseCodes;

		IMatchCode(OEnv env, OCode matchCode, ICaseCode... cases) {
			super(env, /* unused */null, matchCode);
			this.caseCodes = cases;
			this.retypeLocal();
		}

		@Override
		public OCode retypeLocal() {
			OType t = null;
			for (ICaseCode caseCode : this.caseCodes) {
				t = caseCode.getType();
				if (!t.isUntyped()) {
					break;
				}
			}
			if (!t.isUntyped()) {
				return this.asType(this.env(), t);
			}
			return this;
		}

		@Override
		public OType getType() {
			return this.caseCodes[0].getType();
		}

		@Override
		public OCode refineType(OEnv env, OType t) {
			for (int i = 0; i < this.caseCodes.length; i++) {
				this.caseCodes[i].refineType(this.env(), t);
			}
			return this;
		}

		@Override
		public OCode asType(OEnv env, OType t) {
			for (int i = 0; i < this.caseCodes.length; i++) {
				this.caseCodes[i].asType(this.env(), t);
			}
			return this;
		}

		@Override
		public OCode asAssign(OEnv env, String name) {
			for (int i = 0; i < this.caseCodes.length; i++) {
				this.caseCodes[i].asAssign(this.env(), name);
			}
			return this;
		}

		@Override
		public OCode desugar() {
			BlockGen block0 = new BlockGen(this.env());
			block0.pushDefine("result_", new DefaultValueCode(this.getType()));
			block0.pushDefine("input_", this.getFirst().boxCode(this.env()));

			block0.pushDefine("cases_", this.env().v(this.getEmbededCaseData()));
			block0.pushDefine("case_", ICase.class);
			block0.pushDefine("matched_", Object[].class);
			block0.pushDefine("workingList_", block0.eNew(ArrayList.class));
			block0.pushDefine("unmatched_", this.env().v(true));

			boolean hasAnyCase = false;
			for (int i = 0; i < this.caseCodes.length; i++) {
				// ODebug.trace("case[%d] %s", i, codes[i].getType());
				block0.push(this.caseCodes[i]);
				if (this.caseCodes[i].iCase instanceof AnyCase) {
					hasAnyCase = true;
					break;
				}
			}
			if (!hasAnyCase && !this.getType().is(void.class)) {
				block0.pushThrow(
						block0.eNew(OrigamiUnmatchException.class, block0.eValue("unmatch %s"), block0.eVar("input_")));
			}
			return block0.desugar();
		}

		ICase[] getEmbededCaseData() {
			ICase[] c = new ICase[this.caseCodes.length];
			for (int i = 0; i < c.length; i++) {
				c[i] = this.caseCodes[i].iCase;
			}
			return c;
		}
	}

	public static class ICaseCode extends OLocalCode<OEnv> {
		private int index;
		ICase iCase;

		ICaseCode(OEnv env, int index, ICase iCase, OCode condCode, OCode bodyCode) {
			super(env, null/* unused */, condCode, bodyCode);
			this.index = index;
			this.iCase = iCase;
		}

		private OEnv env() {
			return this.getHandled();
		}

		@Override
		public OType getType() {
			return this.nodes[1].getType();
		}

		@Override
		public OCode refineType(OEnv env, OType t) {
			this.nodes[1] = this.nodes[1].refineType(this.env(), t);
			return this;
		}

		@Override
		public OCode asType(OEnv env, OType t) {
			this.nodes[1] = this.nodes[1].asType(this.env(), t);
			return this;
		}

		@Override
		public OCode asAssign(OEnv env, String name) {
			this.nodes[1] = this.nodes[1].asAssign(this.env(), name);
			return this;
		}

		IfCode desugarIfCode(BlockGen block0, int index) {
			return new IfCode(this.env(), //
					block0.eVar("unmatched_"), //
					this.desugarIfThenBlock(block0), //
					new EmptyCode(this.env()));
		}

		OCode desugarIfThenBlock(BlockGen block0) {
			BlockGen block1 = new BlockGen(block0);
			HashMap<String, OType> lvarMap = new HashMap<>();
			this.iCase.updateLocalVariable(null, lvarMap);
			if (lvarMap.size() > 0) {
				for (Map.Entry<String, OType> e : lvarMap.entrySet()) {
					String name = e.getKey();
					OType type = e.getValue();
					block1.pushDefine(name, type);
				}
			}
			block1.pushAssign("case_", block1.eGetIndex(block1.eVar("cases_"), this.index));
			block1.pushAssign("matched_", block1.eMethod(block1.eVar("case_"), "match0", block1.eVar("input_"),
					block1.eVar("workingList_", ArrayList.class)));

			block1.pushIf(block1.eBin(block1.eVar("matched_"), "!=", block1.eNull()), this.desugarGuardBlock(this.env(),
					block1, this.iCase, lvarMap, this.getFirst(), this.getParams()[1]), block1.eEmpty());
			return block1.desugar();
		}

		private OCode desugarGuardBlock(OEnv env, BlockGen block1, ICase iCase, HashMap<String, OType> lvarMap,
				OCode condCode, OCode thenCode) {
			BlockGen block2 = new BlockGen(block1);
			List<String> nameList = iCase.listNames(new ArrayList<String>());
			int i = 0;
			for (String name : nameList) {
				OType ty = lvarMap.get(name);
				block2.pushAssign(name, block2.eGetIndex(block2.eVar("matched_"), i).asType(this.env(), ty));
				i++;
			}
			block2.pushIf(condCode, this.desugarBodyBlock(env, block2, thenCode), block2.eEmpty());
			return block2.desugar();
		}

		private OCode desugarBodyBlock(OEnv env, BlockGen block2, OCode thenCode) {
			BlockGen block3 = new BlockGen(block2);
			block3.push(thenCode);
			block3.pushAssign("unmatched_", env.v(false));
			return block3.desugar();
		}

	}

	// public class MatchCode extends BlockGen {
	// private CaseCode cases[] = null;
	//
	// protected MatchCode(OEnv env, OCode matchCode, Tree<?> body) {
	// super(env, null, matchCode);
	// this.parseCases(env, body);
	// }
	//
	// private void parseCases(OEnv env, Tree<?> body) {
	// this.cases = new CaseCode[body.size()];
	// for (int i = 0; i < this.cases.length; i++) {
	// Tree<?> t = body.get(i);
	// ICase iCase = MatchRules.this.parseCaseBody(env,
	// t/* .get(_expr) */);
	// OEnv lenv = env.newEnv();
	// HashMap<String, OType> varMap = new HashMap<>();
	// iCase.updateLocalVariable(null, varMap);
	// for (Map.Entry<String, OType> v : varMap.entrySet()) {
	// lenv.add(v.getKey(), new OLocalVariable(true, v.getKey(), v.getValue()));
	// }
	// OCode condCode = MatchRules.this.typeCondition(lenv, t.get(_cond, null));
	// OCode bodyCode = MatchRules.this.typeExprOrErrorCode(lenv, t.get(_body));
	// this.cases[i] = new CaseCode(lenv, this, i, iCase, condCode, bodyCode);
	// ODebug.trace("varMap=%s => %s", varMap, this.cases[i].getType());
	// }
	// OType t = env.t(OUntypedType.class);
	// for (CaseCode c : this.cases) {
	// if (t.isUntyped()) {
	// t = c.getType();
	// }
	// }
	// this.asType(this.env(), t);
	// }
	//
	// @Override
	// public OType getType() {
	// OType t = this.getFirst().getType();
	// if (t.isUntyped()) {
	// for (CaseCode c : this.cases) {
	// // ODebug.trace("case => %s", c.getType());
	// if (t.isUntyped()) {
	// t = c.getType();
	// }
	// }
	// if (!t.isUntyped()) {
	// this.asType(this.env(), t);
	// }
	// }
	// return t;
	// }
	//
	// @Override
	// public OCode refineType(OEnv env, OType t) {
	// if (!t.isUntyped()) {
	// this.setType(t);
	// for (int i = 0; i < this.cases.length; i++) {
	// this.cases[i].refineType(this.env(), t);
	// }
	// }
	// return this;
	// }
	//
	// @Override
	// public OCode asType(OEnv env, OType t) {
	// if (!t.isUntyped()) {
	// this.setType(t);
	// for (int i = 0; i < this.cases.length; i++) {
	// this.cases[i].asType(this.env(), t);
	// }
	// }
	// return this;
	// }
	//
	// @Override
	// public OCode desugar() {
	// this.setLabel("L_", "result_", new ODefaultValueCode(this.getType()));
	// this.pushDefine("input_", this.getFirst().boxCode(this.env()));
	//
	// this.pushDefine("cases_", this.env().v(this.cases()));
	// this.pushDefine("case_", ICase.class);
	// this.pushDefine("matched_", Object[].class);
	// this.pushDefine("workingList_", this.eNew(ArrayList.class));
	// boolean hasAnyCase = false;
	// for (int i = 0; i < this.cases.length; i++) {
	// // ODebug.trace("case[%d] %s", i, codes[i].getType());
	// this.push(this.cases[i]);
	// if (this.cases[i].iCase instanceof AnyCase) {
	// hasAnyCase = true;
	// break;
	// }
	// }
	// if (!hasAnyCase && !this.getType().is(void.class)) {
	// this.pushThrow(
	// this.eNew(OrigamiUnmatchException.class, this.eValue("unmatch %s"),
	// this.eVar("input_")));
	// }
	// return super.desugar();
	// }
	//
	// ICase[] cases() {
	// ICase[] c = new ICase[this.cases.length];
	// for (int i = 0; i < c.length; i++) {
	// c[i] = this.cases[i].iCase;
	// }
	// return c;
	// }
	//
	// }
	//
	// public class CaseCode extends BlockGen {
	//
	// private int index;
	// ICase iCase;
	//
	// public CaseCode(OEnv env, BlockGen parent, int index, ICase iCase, OCode
	// condCode, OCode bodyCode) {
	// super(env, parent, condCode, bodyCode);
	// this.index = index;
	// this.iCase = iCase;
	// }
	//
	// @Override
	// public OType getType() {
	// return this.nodes[1].getType();
	// }
	//
	// @Override
	// public OCode refineType(OEnv env, OType t) {
	// if (!t.isUntyped()) {
	// this.nodes[1] = this.nodes[1].refineType(this.env(), t);
	// }
	// return this;
	// }
	//
	// @Override
	// public OCode asType(OEnv env, OType t) {
	// if (!t.isUntyped()) {
	// this.nodes[1] = this.nodes[1].asType(this.env(), t);
	// }
	// return this;
	// }
	//
	// @Override
	// public OCode desugar() {
	// HashMap<String, OType> lvarMap = new HashMap<>();
	// this.iCase.updateLocalVariable(null, lvarMap);
	// if (lvarMap.size() > 0) {
	// for (Map.Entry<String, OType> e : lvarMap.entrySet()) {
	// String name = e.getKey();
	// OType type = e.getValue();
	// this.pushDefine(name, type);
	// }
	// }
	// this.pushAssign("case_", this.eGetIndex(this.eVar("cases_"),
	// this.index));
	// this.pushAssign("matched_", this.eMethod(this.eVar("case_"), "match0",
	// this.eVar("input_"),
	// this.eVar("workingList_", ArrayList.class)));
	//
	// this.pushIf(this.eBin(this.eVar("matched_"), "!=", this.eNull()),
	// new MatchGuardCode(this.env(), this, this.iCase, lvarMap,
	// this.getFirst(), this.getParams()[1]),
	// this.eEmpty());
	// return super.desugar();
	// }
	//
	// }
	//
	// static class MatchGuardCode extends BlockGen {
	// final ICase iCase;
	// final HashMap<String, OType> lvarMap;
	// final OCode condCode;
	// final OCode thenCode;
	//
	// public MatchGuardCode(OEnv env, BlockGen parent, ICase c, HashMap<String,
	// OType> lvarMap, OCode condCode,
	// OCode thenCode) {
	// super(env, parent);
	// this.iCase = c;
	// this.condCode = condCode;
	// this.thenCode = thenCode;
	// this.lvarMap = lvarMap;
	// }
	//
	// @Override
	// public OCode desugar() {
	// List<String> nameList = this.iCase.listNames(new ArrayList<String>());
	// int i = 0;
	// for (String name : nameList) {
	// OType ty = this.lvarMap.get(name);
	// this.pushAssign(name, this.eGetIndex(this.eVar("matched_"),
	// i).asType(this.env(), ty));
	// i++;
	// }
	// OCode bodyCode = new OMultiCode(this.assign("result_", this.thenCode),
	// new OBreakCode(this.env(), this.realName("L_")));
	// this.pushIf(this.condCode, bodyCode, this.eEmpty());
	// return super.desugar();
	// }
	// }

	// private ICase parseCaseBody(OEnv env, Tree<?> tbody) {
	// OTypeSystem ts = env.getTypeSystem();
	// Tree<?> t = tbody.get(_expr);
	// String name = t.getText(_name, null);
	// String tag = t.getTag().getSymbol();
	// switch (tag) {
	// case "AnyCase": {
	// // String name = t.getText(_name, null); // FIXME
	// return new AnyCase();
	// }
	// case "ValueCase": {
	// if (t.has(_list)) {
	// Object[] v = new Object[t.size(_list, 0)];
	// int i = 0;
	// for (Tree<?> e : t.get(_list)) {
	// v[i] = this.parseConstantValue(env, e);
	// if (i > 0) {
	// if (!v[i - 1].getClass().equals(v[i].getClass())) {
	// throw new OErrorCode(env, e, OFmt.S_must_be_S, e.toString(),
	// ts.valueType(v[0]));
	// }
	// }
	// i++;
	// }
	// OType ty = ts.valueType(v[0]);
	// return new ValuesCase(ty, name, v);
	// }
	// Object v = this.parseConstantValue(env, t.get(_value));
	// OType ty = env.getTypeSystem().valueType(v);
	// // ODebug.trace("t=%s, v=%s", t, v);
	// return new ValueCase(ty, name, v);
	// }
	// case "RangeCase":
	// case "RangeUntilCase": {
	// Object start = this.parseConstantValue(env, t.get(_start));
	// Object end = this.parseConstantValue(env, t.get(_end));
	// OType ty = ts.valueType(start);
	// if (!start.getClass().equals(end.getClass())) {
	// throw new OErrorCode(env, t.get(_end), OFmt.S_must_be_S, end, ty);
	// }
	// if (start instanceof Number) {
	// return new DRangeValue(ty, name, (Number) start, (Number) end,
	// !tag.equals("RangeUntilCase"));
	// }
	// if (start instanceof OUnit<?>) {
	// return new DRangeValue(ty, name, (OUnit<?>) start, (OUnit<?>) end,
	// !tag.equals("RangeUntilCase"));
	// }
	// if (start instanceof String) {
	// return new SRangeValue(ty, name, (String) start, (String) end,
	// !tag.equals("RangeUntilCase"));
	// }
	// throw new OErrorCode(env, t, "illenal range values %s", t.toString());
	// }
	// case "TypeCase": {
	// OType type = this.parseParamType(env, t, name, t.get(_type, null), null);
	// ICase inner = this.parseCaseBody(env, t.get(_body, null));
	// // ODebug.trace("name=%s, type=%s", name, type);
	// return new TypeCase(type, name, inner);
	// }
	// case "ExtractCase": {
	// ICase[] l = new ICase[t.size()];
	// int i = 0;
	// for (Tree<?> e : t) {
	// l[i] = this.parseCaseBody(env, e);
	// i++;
	// }
	// return new ExtractCase(env.getStartPoint(), l);
	// }
	// case "ListMoreCase":
	// case "ListCase": {
	// ICase[] l = new ICase[t.size()];
	// int i = 0;
	// for (Tree<?> e : t) {
	// l[i] = this.parseCaseBody(env, e);
	// i++;
	// }
	// return tag.equals("ListMoreCase") ? new ListMoreCase(l, t.size()) : new
	// ListCase(l, t.size());
	// }
	// case "FieldCase": {
	// ICase[] l = new ICase[t.size()];
	// int i = 0;
	// for (Tree<?> e : t) {
	// l[i] = this.parseCaseBody(env, e);
	// if (l[i].getName() == null) {
	// throw new OErrorCode(env, e, "no field name %s", e.toText());
	// }
	// i++;
	// }
	// return new FieldCase(l);
	// }
	// default:
	// throw new OErrorCode(env, t, OFmt.undefined_syntax__YY0,
	// t.getTag().getSymbol());
	// }
	// }

	public static abstract class ICase implements Case, StringCombinator {
		String name;
		OType type;
		boolean nullAble;

		ICase(OType type, String name, boolean nullAble) {
			this.type = type;
			this.name = name;
			this.nullAble = nullAble;
		}

		public void updateLocalVariable(HashMap<String, OType> parent, HashMap<String, OType> varMap) {
			if (!this.isCaptured()) {
				return;
			}
			if (parent != null) {
				OType t = parent.get(this.getName());
				if (t != null && t.eq(this.getType())) {
					return;
				}
			}
			OType t = varMap.get(this.getName());
			if (t == null) {
				varMap.put(this.getName(), this.getType());
			}
		}

		public List<String> listNames(List<String> l) {
			if (this.isCaptured()) {
				l.add(this.getName());
			}
			return l;
		}

		public String getName() {
			return this.name;
		}

		public OType getType() {
			return this.type;
		}

		public boolean isCaptured() {
			return this.getName() != null && this.getType() != null;
		}

		public boolean matched(Object target, List<Object> matched) {
			if (this.isCaptured()) {
				matched.add(target);
			}
			return true;
		}

		public final Object[] match0(Object target, List<Object> matched) {
			matched.clear();
			if (this.match(target, matched)) {
				return matched.toArray(new Object[matched.size()]);
			}
			return null;
		}

		@Override
		public boolean match(Object target, List<Object> matched) {
			// ODebug.trace("target=%s, nullAble=%s", target, nullAble);
			if (this.nullAble) {
				if (target == null) {
					return this.matched(target, matched);
				}
			}
			if (target == null) {
				return false;
			}
			return this.matchExists(target, matched);
		}

		abstract boolean matchExists(Object traget, List<Object> matched);

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			this.appendName(sb);
			StringCombinator.append(sb, this);
			return sb.toString();
		}

		protected void appendName(StringBuilder sb) {
			if (ODebug.isDebug()) {
				sb.append("(");
				sb.append(this.getType());
				sb.append(")");
			}
			if (this.getName() != null) {
				sb.append(this.getName());
				sb.append(": ");
			}
		}
	}

	static class AnyCase extends ICase {

		AnyCase() {
			super(null, null, true);
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			return this.matched(target, matched);
		}

		@Override
		public OType getType() {
			return null;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("_");
		}
	}

	static class TypeCase extends ICase {
		ICase inner;

		public TypeCase(OType ty, String name, ICase inner) {
			super(ty, name, ty instanceof NullableType);
			this.inner = inner;
		}

		@Override
		public void updateLocalVariable(HashMap<String, OType> parent, HashMap<String, OType> varMap) {
			super.updateLocalVariable(parent, varMap);
			if (this.inner != null) {
				this.inner.updateLocalVariable(parent, varMap);
			}
		}

		@Override
		public List<String> listNames(List<String> l) {
			super.listNames(l);
			if (this.inner != null) {
				this.inner.listNames(l);
			}
			return l;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (this.type.isInstance(target)) {
				this.matched(target, matched);
				if (this.inner != null) {
					return this.inner.match(target, matched);
				}
				return true;
			}
			return false;
		}

		@Override
		public void strOut(StringBuilder sb) {
			StringCombinator.append(sb, this.getType());
			if (this.inner != null) {
				StringCombinator.append(sb, this.inner);
			}
		}

	}

	// case 1 => it: int

	static class ValueCase extends ICase {
		private Object value;

		public ValueCase(OType ty, String name, Object v) {
			super(ty, name, v == null);
			this.value = v;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (Objects.equals(this.value, target)) {
				return this.matched(target, matched);
			}
			return false;
		}

		@Override
		public void strOut(StringBuilder sb) {
			StringCombinator.appendQuoted(sb, this.value);
		}

	}

	// case 1|2|3 => it

	static class ValuesCase extends ICase {
		private Object[] values;

		public ValuesCase(OType ty, String name, Object[] array) {
			super(ty, name, false);
			this.values = array;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			for (Object value : this.values) {
				if (value.equals(target)) {
					return this.matched(target, matched);
				}
			}
			return false;
		}

		@Override
		public void strOut(StringBuilder sb) {
			for (int i = 0; i < this.values.length; i++) {
				if (i > 0) {
					sb.append("|");
				}
				StringCombinator.appendQuoted(sb, this.values[i]);
			}
		}

	}

	static class DRangeValue extends ICase {
		double start;
		double end;
		boolean inclusive;

		public DRangeValue(OType ty, String name, Number start, Number end, boolean inclusive) {
			super(ty, name, false);
			this.start = start.doubleValue();
			this.end = end.doubleValue();
			this.inclusive = inclusive;
		}

		public DRangeValue(OType ty, String name, OUnit<?> start, OUnit<?> end, boolean inclusive) {
			super(ty, name, false);
			this.start = start.doubleValue();
			this.end = end.doubleValue();
			this.inclusive = inclusive;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (target instanceof Number) {
				return this.matchExists(target, ((Number) target).doubleValue(), matched);
			}
			if (target instanceof OUnit<?>) {
				return this.matchExists(target, ((OUnit<?>) target).doubleValue(), matched);
			}
			return false;
		}

		private boolean matchExists(Object target, double d, List<Object> matched) {
			if (d < this.start) {
				return false;
			}
			if (this.inclusive) {
				if (this.end < d) {
					return false;
				}
			} else {
				if (this.end <= d) {
					return false;
				}
			}
			return this.matched(target, matched);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("(");
			StringCombinator.appendQuoted(sb, this.start);
			sb.append(" to ");
			if (!this.inclusive) {
				sb.append("<");
			}
			StringCombinator.appendQuoted(sb, this.end);
			sb.append(")");
		}

	}

	static class SRangeValue extends ICase {
		String start;
		String end;
		boolean inclusive;

		public SRangeValue(OType ty, String name, String start, String end, boolean inclusive) {
			super(ty, name, false);
			this.start = start;
			this.end = end;
			this.inclusive = inclusive;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (target == null) {
				return false;
			}
			String s = target.toString();
			if (s.compareTo(this.start) < 0) {
				return false;
			}
			if (this.inclusive) {
				if (this.end.compareTo(s) < 0) {
					return false;
				}
			} else {
				if (this.end.compareTo(s) <= 0) {
					return false;
				}
			}
			return this.matched(target, matched);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("(");
			StringCombinator.appendQuoted(sb, this.start);
			sb.append(" to ");
			if (!this.inclusive) {
				sb.append("<");
			}
			StringCombinator.appendQuoted(sb, this.end);
			sb.append(")");
		}

	}

	static abstract class NestedCase extends ICase {
		ICase[] innerCases;

		public NestedCase(OType ty, ICase[] list) {
			super(ty, null, false);
			this.innerCases = list;
		}

		boolean matchInner(SequenceExtractable<?> se, List<Object> matched) {
			int index = 0;
			for (ICase inner : this.innerCases) {
				if (!inner.match((index), matched)) {
					return false;
				}
				index++;
			}
			return true;
		}

		@Override
		public void updateLocalVariable(HashMap<String, OType> parent, HashMap<String, OType> varMap) {
			super.updateLocalVariable(parent, varMap);
			for (ICase inner : this.innerCases) {
				inner.updateLocalVariable(parent, varMap);
			}
		}

		@Override
		public List<String> listNames(List<String> l) {
			for (ICase inner : this.innerCases) {
				inner.listNames(l);
			}
			return l;
		}

		void appendInner(StringBuilder sb, String s, String e) {
			sb.append(s);
			for (int i = 0; i < this.innerCases.length; i++) {
				if (i > 0) {
					sb.append(",");
				}
				this.innerCases[i].appendName(sb);
				this.innerCases[i].strOut(sb);
			}
			sb.append(e);
		}
	}

	// <A,B,C>
	static class ExtractCase extends NestedCase {
		OEnv env;

		public ExtractCase(OEnv env, ICase[] list) {
			super(null, list);
			this.env = env;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (target instanceof ObjectExtractable) {
				ObjectExtractable t = (ObjectExtractable) target;
				for (ICase inner : this.innerCases) {
					// String name = inner.getName();
					OType ty = inner.getType();
					Object v = t.lookup(ty.unwrap(this.env), ty instanceof NullableType);
					if (v == void.class) {
						return false;
					}
					inner.matched(v, matched);
				}
				return true;
			}
			return false;
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.appendInner(sb, "<", ">");
		}
	}

	// {a:T,b:T,c:T}
	static class FieldCase extends NestedCase {
		public FieldCase(ICase[] list) {
			super(null, list);
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (target instanceof FieldExtractable) {
				FieldExtractable t = (FieldExtractable) target;
				for (ICase inner : this.innerCases) {
					String name = inner.getName();
					Object v = t.getf(Symbol.uniqueId(name));
					if (!inner.match(v, matched)) {
						return false;
					}
				}
				return true;
			}
			if (target instanceof Map) {
				Map<?, ?> t = (Map<?, ?>) target;
				for (ICase inner : this.innerCases) {
					String name = inner.getName();
					Object v = t.get(name);
					if (!inner.match(v, matched)) {
						return false;
					}
				}
				return true;
			}
			for (ICase inner : this.innerCases) {
				String name = inner.getName();
				Object v = loadFieldValue(target, name);
				if (!inner.match(v, matched)) {
					return false;
				}
			}
			return true;
		}

		public static Object loadFieldValue(Object target, String name) {
			try {
				Field f = target.getClass().getField(name);
				return f.get(target);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				return null;
			}
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.appendInner(sb, "{", "}");
		}

	}

	// [X,Y,Z]
	static class ListCase extends NestedCase {
		int size;

		public ListCase(ICase[] list, int size) {
			super(null, list);
			this.size = size;
		}

		protected boolean checkSize(int targetSize) {
			return this.size == targetSize;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (target.getClass().isArray()) {
				if (!this.checkSize(Array.getLength(target))) {
					return false;
				}
				int i = 0;
				for (ICase inner : this.innerCases) {
					Object v = Array.get(target, i);
					if (!inner.match(v, matched)) {
						return false;
					}
					i++;
				}
				return true;
			}
			if (target instanceof ListExtractable<?>) {
				ListExtractable<?> l = (ListExtractable<?>) target;
				if (!this.checkSize(Array.getLength(l.size()))) {
					return false;
				}
				int i = 0;
				for (ICase inner : this.innerCases) {
					Object v = l.geti(i);
					if (!inner.match(v, matched)) {
						return false;
					}
					i++;
				}
				return true;
			}
			if (target instanceof List<?>) {
				List<?> l = (List<?>) target;
				if (!this.checkSize(Array.getLength(l.size()))) {
					return false;
				}
				int i = 0;
				for (ICase inner : this.innerCases) {
					Object v = l.get(i);
					if (!inner.match(v, matched)) {
						return false;
					}
					i++;
				}
				return true;
			}
			return false;
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.appendInner(sb, "[", "]");
		}

	}

	// [X,Y,Z,...]

	static class ListMoreCase extends ListCase {
		int size;

		public ListMoreCase(ICase[] list, int size) {
			super(list, size);
		}

		@Override
		protected boolean checkSize(int targetSize) {
			return this.size <= targetSize;
		}

		@Override
		public void strOut(StringBuilder sb) {
			this.appendInner(sb, "[", ",...]");
		}

	}

	// Exception

	@SuppressWarnings("serial")
	public static class OrigamiUnmatchException extends OrigamiException {

		public OrigamiUnmatchException(String fmt, Object value) {
			super(fmt, value);
		}

	}

}
