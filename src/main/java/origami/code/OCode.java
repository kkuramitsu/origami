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

package origami.code;

import java.util.List;

import origami.OEnv;
import origami.OEnv.OListMatcher;
import origami.asm.OAsm;
import origami.asm.OCallSite;
import origami.ffi.OCast;
import origami.lang.callsite.OFuncCallSite;
import origami.lang.callsite.OGetterCallSite;
import origami.lang.callsite.OMethodCallSite;
import origami.nez.ast.SourcePosition;
import origami.rule.OFmt;
import origami.type.OType;
import origami.type.OTypeSystem;

public interface OCode {

	public boolean isUntyped();

	public OCode retypeLocal();

	public OCode retypeAll(int[] c);

	public OCode refineType(OEnv env, OType req);

	public OType getType();

	public OCode[] getParams();

	public void find(List<OCode> l, OListMatcher<OCode> mat);

	public int getMatchCost();

	public Object eval(OEnv env) throws Throwable;

	public void generate(OGenerator gen);

	public OType valueType();

	public OCode setSourcePosition(SourcePosition s);

	public SourcePosition getSourcePosition();

	public boolean isDefined();

	public boolean hasReturnCode();

	public OCode thisCode();

	/* default constructor */

	public default OTypeSystem getTypeSystem() {
		return this.getType().getTypeSystem();
	}

	public default OCode asType(OEnv env, OType req) {
		OCode node = req.accept(env, this, OType.EmptyTypeChecker);
		if (node != this) {
			// ODebug.trace("asType %s %s", req, node);
		}
		return node;
	}

	public default OCode asType(OEnv env, Class<?> c) {
		return this.asType(env, env.t(c));
	}

	// Code Construction

	default OCode[] cons(OCode... params) {
		OCode[] p = new OCode[params.length + 1];
		p[0] = this;
		System.arraycopy(params, 0, p, 1, params.length);
		return p;
	}

	// Code(params)
	public default OCode newApplyCode(OEnv env, OCode... params) {
		String funcName = thisCode().getType().getFuncName();
		return this.newMethodCode(env, funcName, params);
	}

	// Code.name(params)
	public default OCode newMethodCode(OEnv env, String name, OCode... params) {
		OCode[] p = cons(params);
		return OCallSite.findParamCode(env, OMethodCallSite.class, name, p);
	}

	// Code $op param (e.g., code + code)
	public default OCode newBinaryCode(OEnv env, String op, OCode params) {
		OCode[] p = cons(params);
		return OCallSite.findParamCode(env, OFuncCallSite.class, op, p);
	}

	// Code $op (e.g., !code)
	public default OCode newUnaryCode(OEnv env, String op) {
		return OCallSite.findParamCode(env, OFuncCallSite.class, op, this);
	}

	// Code.name
	public default OCode newGetterCode(OEnv env, String name) {
		return OCallSite.findParamCode(env, OGetterCallSite.class, name, this);
	}

	// Code = right
	public default OCode newAssignCode(OEnv env, OType type, OCode right) {
		throw new OErrorCode(env, OFmt.YY0_is_not_assignable, this.getClass().getSimpleName());
	}

	// public default OCode newIteratorCode(OEnv env) {
	// return OrigamiIterator.newIteratorCode(env, this);
	// }

	// (t)Code
	public default OCode newCastCode(OEnv env, OType t) {
		return t.newCastCode(env, this);
	}

	// (c)Code
	public default OCode newCastCode(OEnv env, Class<?> c) {
		return this.newCastCode(env, env.getTypeSystem().newType(c));
	}

	// (t)Code
	public default OCode boxCode(OEnv env) {
		OType f = this.getType();
		if (f.isPrimitive()) {
			OType t = f.boxType();
			// OConv conv = OConv.getConv(env, f, t);
			return new OCastCode(t, OCast.SAME, this);
		}
		return this;
	}

	public default OCode checkAcc(OEnv env) {
		return this;
	}

}
