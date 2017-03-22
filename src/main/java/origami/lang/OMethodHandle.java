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

package origami.lang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import origami.OEnv;
import origami.asm.OCallSite;
import origami.code.OCode;
import origami.ffi.OCast;
import origami.type.OType;
import origami.type.OTypeSystem;
import origami.type.OUntypedType;
import origami.util.OArrayUtils;
import origami.util.ODebug;
import origami.util.StringCombinator;
import origami.type.OParamMatcher;

public interface OMethodHandle {

	public final static int DynamicInvocation = 0;
	public final static int StaticInvocation = 1;
	public final static int SpecialInvocation = 2;
	public final static int VirtualInvocation = 3;
	public final static int InterfaceInvocation = 4;
	public final static int StaticGetter = 5;
	public final static int VirtualGetter = 6;
	public final static int StaticSetter = 7;
	public final static int VirtualSetter = 8;

	public final static OMethodHandle[] emptyMethods = new OMethodHandle[0];

	public OTypeSystem getTypeSystem();

	public default boolean isPure() {
		return false;
	}

	public boolean isPublic();

	public boolean isStatic();

	public boolean isDynamic();

	public boolean isSpecial();

	public int getInvocation();

	public boolean isMutable();

	public default boolean isVarArgs() {
		return false;
	}

	public default MethodType methodType() {
		// OType[] t = this.isSpecial() ? this.getParamTypes() :
		// this.getThisParamTypes();
		OType[] t = this.getParamTypes();
		Class<?>[] p = new Class<?>[t.length];
		for (int i = 0; i < t.length; i++) {
			p[i] = t[i].unwrapOrNull(Object.class);
		}
		return MethodType.methodType(this.getReturnType().unwrapOrNull(Object.class), p);
	}

	public OType getDeclaringClass();

	public OType getReturnType();

	public default void setLocalName(String name) {

	}

	public default String getLocalName() {
		return this.getName();
	}

	public String getName();

	public default boolean matchName(String name) {
		return name.equals(this.getLocalName());
	}

	public default int getParamSize() {
		return this.getParamTypes().length;
	}

	public default int getThisParamSize() {
		if (!this.isStatic()) {
			return this.getParamSize() + 1;
		}
		return this.getParamSize();
	}

	public default boolean isThisParamSize(int psize) {
		int tsize = this.getThisParamSize();
		// ODebug.trace("%d %d", tsize, psize);
		return (tsize == psize || (this.isVarArgs() && tsize - 1 <= psize));
	}

	public default boolean matchThisParams(OType[] p) {
		if (p.length == this.getThisParamSize()) {
			OType[] pp = this.getThisParamTypes();
			for (int i = 0; i < p.length; i++) {
				if (!p[i].eq(pp[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public default boolean matchThisParams(Class<?>[] p) {
		if (p.length == this.getThisParamSize()) {
			OType[] pp = this.getThisParamTypes();
			for (int i = 0; i < p.length; i++) {
				if (!pp[i].is(p[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public default String[] getParamNames() {
		return new String[this.getParamSize()]; // fixme
	}

	public String[] getThisParamNames();

	public OType[] getParamTypes();

	public OType[] getThisParamTypes();

	public OType[] getExceptionTypes();

	public default OCode matchParamCode(OEnv env, OCallSite site, OCode... params) {
		OType[] p = this.getThisParamTypes();
		OParamMatcher mat = new OParamMatcher(env, this);
		params = mat.transformParams(this.isVarArgs(), p.length, params);
		int cost = mat.tryMatch(this.isPure(), p, params);
		OType ret = this.getReturnType();
		if (ret instanceof OUntypedType) {
			this.getDecl().typeCheck(env);
			ret = this.getReturnType();
		}
		ret = ret.resolveVarType(mat);
		// ODebug.trace("** found %d ret=%s %s", cost, ret, this);
		return this.newMatchedParamCode(env, site, ret, params, cost);
	}

	public default OMethodDecl getDecl() {
		ODebug.NotAvailable();
		return null;
	}

	public default Object eval(OEnv env, Object... values) throws Throwable {
		ODebug.NotAvailable(this);
		return null;
	}

	public default boolean isMethodType(MethodType mt) {
		if (mt.parameterCount() != this.getThisParamSize()) {
			return false;
		}
		if (mt.returnType() != this.getReturnType().unwrapOrNull((Class<?>) null)) {
			return false;
		}
		OType[] p = this.getThisParamTypes();
		for (int i = 0; i < p.length; i++) {
			if (mt.parameterType(i) != p[i].unwrapOrNull((Class<?>) null)) {
				return false;
			}
		}
		return true;
	}

	public default MethodHandle getMethodHandle(OEnv env, Lookup lookup) throws Throwable {
		ODebug.NotAvailable(this);
		return null;
	}

	public default OCode newMethodCode(OEnv env, OCode... params) {
		return newMatchedParamCode(env, null, this.getReturnType(), params, OCast.SAME);
	}

	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost);

	public default Object getCallSite() {
		ODebug.NotAvailable(this);
		return null;
	}

	public default Object[] getCallSiteParams() {
		ODebug.NotAvailable(this);
		return null;
	}

}

abstract class OCommonMethodHandle implements OMethodHandle, StringCombinator, OArrayUtils {

	@Override
	public String[] getThisParamNames() {
		if (!this.isStatic()) {
			return append("this", this.getParamNames());
		}
		return this.getParamNames();
	}

	@Override
	public OType[] getThisParamTypes() {
		OType[] p = this.getParamTypes();
		if (!isStatic()) {
			p = append(this.getDeclaringClass().toGenericType(), p);
		}
		return p;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.getName());
		for (OType p : this.getThisParamTypes()) {
			sb.append(" ");
			StringCombinator.append(sb, p);
		}
		sb.append(" -> ");
		StringCombinator.append(sb, this.getReturnType());
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}
}
