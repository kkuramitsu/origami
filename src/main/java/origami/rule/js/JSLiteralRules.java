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

package origami.rule.js;

import java.math.BigDecimal;
import java.math.BigInteger;

import origami.code.OArrayCode;
import origami.code.OCode;
import origami.code.OValueCode;
import origami.ffi.OImportable;
import origami.lang.OEnv;
import origami.nez.ast.Tree;
import origami.rule.TypeRule;
import origami.rule.TypeAnalysis;
import origami.type.AnyType;
import origami.type.OArrayType;
import origami.type.OType;
import origami.util.OStringUtils;
import origami.util.OTypeRule;
import origami.util.OLog.Messenger;

public class JSLiteralRules implements OImportable, TypeAnalysis {

	public OTypeRule TrueExpr = new Value(new Boolean(true), Boolean.class);
	public OTypeRule FalseExpr = new Value(new Boolean(false), Boolean.class);

	public OTypeRule NumberExpr = new Number(Double.class);
	public OTypeRule ByteExpr = new Number(Byte.class);
	public OTypeRule ShortExpr = new Number(Short.class);
	public OTypeRule IntExpr = new Number(Integer.class);
	public OTypeRule LongExpr = new Number(Long.class);
	public OTypeRule FloatExpr = new Number(Float.class);
	public OTypeRule DoubleExpr = new Number(Double.class);

	public OTypeRule CharExpr = new Number(float.class); // FIXME: importing
															// from
	// CharExpr
	public OTypeRule StringExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return new OValueCode(OStringUtils.unquoteString(t.toText()), env.t(String.class));
		}
	};

	public static class Value extends TypeRule {
		public final Object value;
		public final Class<?> baseType;

		public Value(Object value, Class<?> baseType) {
			this.value = value;
			this.baseType = baseType;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return new OValueCode(this.value, env.t(this.baseType));
		}
	}

	public static class Number extends TypeRule implements TypeAnalysis {
		public final Class<?> baseType;

		public Number(Class<?> baseType) {
			this.baseType = baseType;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			// if (this.checkValueFunc(env, node)) {
			// return node;
			// }
			Messenger m = new Messenger();
			String text = t.toText().replace("_", "");
			int radix = 10;
			if (text.endsWith("L") || text.endsWith("l")) {
				text = text.substring(0, text.length() - 1);
			}
			if (text.startsWith("0b") || text.startsWith("0B")) {
				text = text.substring(2);
				radix = 2;
			} else if (text.startsWith("0x") || text.startsWith("0X")) {
				text = text.substring(2);
				radix = 16;
			} else if (text.startsWith("0")) {
				radix = 8;
			}
			Class<?> base = baseType;
			Object value = null;
			if (base == Integer.class) {
				try {
					value = Integer.parseInt(text, radix);
				} catch (NumberFormatException e) {
					m.reportWarning(t, "wrong number format %s by %s", text, e);
					value = new Integer(0);
				}
			} else if (base == Double.class) {
				try {
					value = Double.parseDouble(text);
				} catch (NumberFormatException e) {
					m.reportWarning(t, "wrong number format %s by %s", text, e);
					value = new Double(0.0);
				}
			} else if (base == Long.class) {
				try {
					value = Long.parseLong(text, radix);
				} catch (NumberFormatException e) {
					m.reportWarning(t, "wrong number format %s by %s", text, e);
					value = new Long(0L);
				}
			} else if (base == Float.class) {
				try {
					value = Float.parseFloat(text);
				} catch (NumberFormatException e) {
					m.reportWarning(t, "wrong number format %s by %s", text, e);
					value = new Float(0.0f);
				}
			} else if (base == BigInteger.class) {
				try {
					value = new BigInteger(text, radix);
				} catch (NumberFormatException e2) {
					m.reportWarning(t, "wrong number format %s by %s", text, e2);
					value = BigInteger.ZERO;
				}
			} else {
				try {
					value = new BigDecimal(text);
				} catch (NumberFormatException e2) {
					m.reportWarning(t, "wrong number format %s by %s", text, e2);
					value = BigDecimal.ZERO;
				}
			}
			OCode code = new OValueCode(value, env.t(base));
			return m.newMessageCode(code);
		}

	}

	public OTypeRule ArrayExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType innerType = null;
			OCode[] arrays = new OCode[t.size()];
			for (int i = 0; i < t.size(); i++) {
				arrays[i] = typeExpr(env, t.get(i));
				OType ctype = arrays[i].getType();
				if (innerType == null) {
					innerType = ctype;
				} else if (innerType != ctype) {
					innerType = innerType.commonSuperType(ctype);
				}
			}
			if (innerType == null) {
				innerType = env.t(AnyType.class);
			}
			for (int i = 0; i < t.size(); i++) {
				arrays[i] = typeCheck(env, innerType, arrays[i]);
			}
			return new OArrayCode(OArrayType.newType(innerType), arrays);
		}
	};

}
