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

import java.lang.invoke.MethodType;

import org.objectweb.asm.Type;

import origami.asm.OCallSite;
import origami.lang.type.OType;
import origami.util.OTypeUtils;

public class OVirtualMethodHandle extends OMethodWrapper {

	private OEnv env;
	private String name;
	private OCallSite site;

	public OVirtualMethodHandle(OEnv env, OCallSite site, String name, OMethodHandle mh) {
		super(mh);
		this.env = env;
		this.name = name;
		this.site = site;
	}

	public OEnv getEnv() {
		return this.env;
	}

	@Override
	public String getLocalName() { // targetName
		return this.name;
	}

	@Override
	public MethodType methodType() {
		OType[] t = mh.isSpecial() ? mh.getParamTypes() : mh.getThisParamTypes();
		Class<?>[] p = new Class<?>[t.length];
		for (int i = 0; i < t.length; i++) {
			p[i] = OTypeUtils.boxType(t[i].unwrapOrNull(Object.class));
		}
		return MethodType.methodType(mh.getReturnType().unwrapOrNull(Object.class), p);
	}

	@Override
	public OCallSite getCallSite() {
		return this.site;
	}

	@Override
	public Object[] getCallSiteParams() {
		return new Object[] { Type.getType(env.findEntryPoint()), OCallSite.Virtual };
	}

}