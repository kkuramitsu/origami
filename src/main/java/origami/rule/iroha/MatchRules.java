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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import origami.ODebug;
import origami.OEnv;
import origami.code.GenerativeCode;
import origami.code.OBreakCode;
import origami.code.OCode;
import origami.code.ODefaultValueCode;
import origami.code.OErrorCode;
import origami.code.OMultiCode;
import origami.ffi.Case;
import origami.ffi.FieldExtractable;
import origami.ffi.ListExtractable;
import origami.ffi.ObjectExtractable;
import origami.ffi.OrigamiException;
import origami.ffi.SequenceExtractable;
import origami.lang.OLocalVariable;
import origami.nez.ast.Symbol;
import origami.nez.ast.Tree;
import origami.rule.TypeRule;
import origami.rule.OFmt;
import origami.rule.OSymbols;
import origami.rule.SyntaxAnalysis;
import origami.rule.unit.OUnit;
import origami.type.NullableType;
import origami.type.OType;
import origami.type.OTypeSystem;
import origami.type.OUntypedType;
import origami.util.OImportable;
import origami.util.OTypeRule;
import origami.util.StringCombinator;

public class MatchRules implements OImportable, OSymbols, SyntaxAnalysis {

	public class MatchCode extends GenerativeCode {
		private CaseCode cases[] = null;

		protected MatchCode(OEnv env, OCode matchCode, Tree<?> body) {
			super(env, null, matchCode);
			parseCases(env, body);
		}

		private void parseCases(OEnv env, Tree<?> body) {
			this.cases = new CaseCode[body.size()];
			for (int i = 0; i < cases.length; i++) {
				Tree<?> t = body.get(i);
				ICase iCase = parseCaseBody(env, t/* .get(_expr) */);
				OEnv lenv = env.newEnv();
				HashMap<String, OType> varMap = new HashMap<>();
				iCase.updateLocalVariable(null, varMap);
				for (Map.Entry<String, OType> v : varMap.entrySet()) {
					lenv.add(v.getKey(), new OLocalVariable(true, v.getKey(), v.getValue()));
				}
				OCode condCode = typeCondition(lenv, t.get(_cond, null));
				OCode bodyCode = typeExprOrErrorCode(lenv, t.get(_body));
				cases[i] = new CaseCode(lenv, this, i, iCase, condCode, bodyCode);
				ODebug.trace("varMap=%s => %s", varMap, cases[i].getType());
			}
			OType t = env.t(OUntypedType.class);
			for (CaseCode c : cases) {
				if (t.isUntyped()) {
					t = c.getType();
				}
			}
			this.asType(env(), t);
		}

		@Override
		public OType getType() {
			OType t = this.getFirst().getType();
			if (t.isUntyped()) {
				for (CaseCode c : this.cases) {
					// ODebug.trace("case => %s", c.getType());
					if (t.isUntyped()) {
						t = c.getType();
					}
				}
				if (!t.isUntyped()) {
					this.asType(env(), t);
				}
			}
			return t;
		}

		@Override
		public OCode refineType(OEnv env, OType t) {
			if (!t.isUntyped()) {
				this.setType(t);
				for (int i = 0; i < cases.length; i++) {
					cases[i].refineType(env(), t);
				}
			}
			return this;
		}

		@Override
		public OCode asType(OEnv env, OType t) {
			if (!t.isUntyped()) {
				this.setType(t);
				for (int i = 0; i < cases.length; i++) {
					cases[i].asType(env(), t);
				}
			}
			return this;
		}

		@Override
		public OCode desugar() {
			this.setLabel("L_", "result_", new ODefaultValueCode(this.getType()));
			this.pushDefine("input_", this.getFirst().boxCode(env()));

			this.pushDefine("cases_", env().v(cases()));
			this.pushDefine("case_", ICase.class);
			this.pushDefine("matched_", Object[].class);
			this.pushDefine("workingList_", _new(ArrayList.class));
			boolean hasAnyCase = false;
			for (int i = 0; i < cases.length; i++) {
				// ODebug.trace("case[%d] %s", i, codes[i].getType());
				push(cases[i]);
				if (cases[i].iCase instanceof AnyCase) {
					hasAnyCase = true;
					break;
				}
			}
			if (!hasAnyCase && !this.getType().is(void.class)) {
				pushThrow(_new(OrigamiUnmatchException.class, _value("unmatch %s"), _var("input_")));
			}
			return super.desugar();
		}

		ICase[] cases() {
			ICase[] c = new ICase[cases.length];
			for (int i = 0; i < c.length; i++) {
				c[i] = this.cases[i].iCase;
			}
			return c;
		}

	}

	public class CaseCode extends GenerativeCode {

		private int index;
		ICase iCase;

		public CaseCode(OEnv env, GenerativeCode parent, int index, ICase iCase, OCode condCode, OCode bodyCode) {
			super(env, parent, condCode, bodyCode);
			this.index = index;
			this.iCase = iCase;
		}

		@Override
		public OType getType() {
			return this.nodes[1].getType();
		}

		@Override
		public OCode refineType(OEnv env, OType t) {
			if (!t.isUntyped()) {
				this.nodes[1] = this.nodes[1].refineType(env(), t);
			}
			return this;
		}

		@Override
		public OCode asType(OEnv env, OType t) {
			if (!t.isUntyped()) {
				this.nodes[1] = this.nodes[1].asType(env(), t);
			}
			return this;
		}

		@Override
		public OCode desugar() {
			HashMap<String, OType> lvarMap = new HashMap<>();
			iCase.updateLocalVariable(null, lvarMap);
			if (lvarMap.size() > 0) {
				for (Map.Entry<String, OType> e : lvarMap.entrySet()) {
					String name = e.getKey();
					OType type = e.getValue();
					pushDefine(name, type);
				}
			}
			pushAssign("case_", _geti(_var("cases_"), index));
			pushAssign("matched_",
					_method(_var("case_"), "match0", _var("input_"), _var("workingList_", ArrayList.class)));

			pushIf(_bin(_var("matched_"), "!=", _null()),
					new MatchGuardCode(env(), this, iCase, lvarMap, this.getFirst(), this.getParams()[1]), _empty());
			return super.desugar();
		}

	}

	static class MatchGuardCode extends GenerativeCode {
		final ICase iCase;
		final HashMap<String, OType> lvarMap;
		final OCode condCode;
		final OCode thenCode;

		public MatchGuardCode(OEnv env, GenerativeCode parent, ICase c, HashMap<String, OType> lvarMap, OCode condCode,
				OCode thenCode) {
			super(env, parent);
			this.iCase = c;
			this.condCode = condCode;
			this.thenCode = thenCode;
			this.lvarMap = lvarMap;
		}

		@Override
		public OCode desugar() {
			List<String> nameList = iCase.listNames(new ArrayList<String>());
			int i = 0;
			for (String name : nameList) {
				OType ty = lvarMap.get(name);
				pushAssign(name, _geti(_var("matched_"), i).asType(env(), ty));
				i++;
			}
			OCode bodyCode = new OMultiCode(assign("result_", thenCode), new OBreakCode(env(), name("L_")));
			pushIf(condCode, bodyCode, _empty());
			return super.desugar();
		}
	}

	public OTypeRule MatchExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode expr = typeExpr(env, t.get(_expr));
			OCode match = new MatchCode(env, expr, t.get(_body));
			ODebug.trace("*match=%s", match.getType());
			return match;
		}
	};

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
					v[i] = parseConstantValue(env, e);
					if (i > 0) {
						if (!v[i - 1].getClass().equals(v[i].getClass())) {
							throw new OErrorCode(env, e, OFmt.S_must_be_S, e.toString(), ts.valueType(v[0]));
						}
					}
					i++;
				}
				OType ty = ts.valueType(v[0]);
				return new ValuesCase(ty, name, v);
			}
			Object v = parseConstantValue(env, t.get(_value));
			OType ty = env.getTypeSystem().valueType(v);
			// ODebug.trace("t=%s, v=%s", t, v);
			return new ValueCase(ty, name, v);
		}
		case "RangeCase":
		case "RangeUntilCase": {
			Object start = parseConstantValue(env, t.get(_start));
			Object end = parseConstantValue(env, t.get(_end));
			OType ty = ts.valueType(start);
			if (!start.getClass().equals(end.getClass())) {
				throw new OErrorCode(env, t.get(_end), OFmt.S_must_be_S, end, ty);
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
			throw new OErrorCode(env, t, "illenal range values %s", t.toString());
		}
		case "TypeCase": {
			OType type = parseParamType(env, t, name, t.get(_type, null), null);
			ICase inner = parseCaseBody(env, t.get(_body, null));
			// ODebug.trace("name=%s, type=%s", name, type);
			return new TypeCase(type, name, inner);
		}
		case "ExtractCase": {
			ICase[] l = new ICase[t.size()];
			int i = 0;
			for (Tree<?> e : t) {
				l[i] = parseCaseBody(env, e);
				i++;
			}
			return new ExtractCase(env.getStartPoint(), l);
		}
		case "ListMoreCase":
		case "ListCase": {
			ICase[] l = new ICase[t.size()];
			int i = 0;
			for (Tree<?> e : t) {
				l[i] = parseCaseBody(env, e);
				i++;
			}
			return tag.equals("ListMoreCase") ? new ListMoreCase(l, t.size()) : new ListCase(l, t.size());
		}
		case "FieldCase": {
			ICase[] l = new ICase[t.size()];
			int i = 0;
			for (Tree<?> e : t) {
				l[i] = parseCaseBody(env, e);
				if (l[i].getName() == null) {
					throw new OErrorCode(env, e, "no field name %s", e.toText());
				}
				i++;
			}
			return new FieldCase(l);
		}
		default:
			throw new OErrorCode(env, t, OFmt.undefined_syntax__YY0, t.getTag().getSymbol());
		}
	}

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
			if (isCaptured()) {
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
			if (match(target, matched)) {
				return matched.toArray(new Object[matched.size()]);
			}
			return null;
		}

		@Override
		public boolean match(Object target, List<Object> matched) {
			// ODebug.trace("target=%s, nullAble=%s", target, nullAble);
			if (nullAble) {
				if (target == null) {
					return matched(target, matched);
				}
			}
			if (target == null) {
				return false;
			}
			return matchExists(target, matched);
		}

		abstract boolean matchExists(Object traget, List<Object> matched);

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			appendName(sb);
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
			return matched(target, matched);
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
				inner.updateLocalVariable(parent, varMap);
			}
		}

		@Override
		public List<String> listNames(List<String> l) {
			super.listNames(l);
			if (this.inner != null) {
				inner.listNames(l);
			}
			return l;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			if (type.isInstance(target)) {
				matched(target, matched);
				if (inner != null) {
					return inner.match(target, matched);
				}
				return true;
			}
			return false;
		}

		@Override
		public void strOut(StringBuilder sb) {
			StringCombinator.append(sb, this.getType());
			if (inner != null) {
				StringCombinator.append(sb, inner);
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
			if (Objects.equals(value, target)) {
				return matched(target, matched);
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
			values = array;
		}

		@Override
		public boolean matchExists(Object target, List<Object> matched) {
			for (Object value : values) {
				if (value.equals(target)) {
					return matched(target, matched);
				}
			}
			return false;
		}

		@Override
		public void strOut(StringBuilder sb) {
			for (int i = 0; i < values.length; i++) {
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
			if (d < start) {
				return false;
			}
			if (inclusive) {
				if (end < d) {
					return false;
				}
			} else {
				if (end <= d) {
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
			if (!inclusive) {
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
			if (s.compareTo(start) < 0) {
				return false;
			}
			if (inclusive) {
				if (end.compareTo(s) < 0) {
					return false;
				}
			} else {
				if (end.compareTo(s) <= 0) {
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
			if (!inclusive) {
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
			for (ICase inner : innerCases) {
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
			for (ICase inner : innerCases) {
				inner.updateLocalVariable(parent, varMap);
			}
		}

		@Override
		public List<String> listNames(List<String> l) {
			for (ICase inner : innerCases) {
				inner.listNames(l);
			}
			return l;
		}

		void appendInner(StringBuilder sb, String s, String e) {
			sb.append(s);
			for (int i = 0; i < innerCases.length; i++) {
				if (i > 0) {
					sb.append(",");
				}
				innerCases[i].appendName(sb);
				innerCases[i].strOut(sb);
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
				for (ICase inner : innerCases) {
					// String name = inner.getName();
					OType ty = inner.getType();
					Object v = t.lookup(ty.unwrap(env), ty instanceof NullableType);
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
				for (ICase inner : innerCases) {
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
				for (ICase inner : innerCases) {
					String name = inner.getName();
					Object v = t.get(name);
					if (!inner.match(v, matched)) {
						return false;
					}
				}
				return true;
			}
			for (ICase inner : innerCases) {
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
				if (!checkSize(Array.getLength(target))) {
					return false;
				}
				int i = 0;
				for (ICase inner : innerCases) {
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
				if (!checkSize(Array.getLength(l.size()))) {
					return false;
				}
				int i = 0;
				for (ICase inner : innerCases) {
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
				if (!checkSize(Array.getLength(l.size()))) {
					return false;
				}
				int i = 0;
				for (ICase inner : innerCases) {
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
