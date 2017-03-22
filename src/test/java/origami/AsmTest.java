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

package origami;

import java.lang.reflect.Method;

import origami.asm.OAnno;
import origami.code.OCode;
import origami.code.OReturnCode;
import origami.lang.OClassDeclType;
import origami.type.OType;
import origami.util.OTypeUtils;

public class AsmTest {

	public Object eval(OEnv env, OCode code) throws Throwable {
		OClassDeclType ct = OClassDeclType.currentType(env);
		ct.addMethod(new OAnno("public,static"), code.getType(), "f", OType.emptyNames, OType.emptyTypes,
				OType.emptyTypes, new OReturnCode(env, code));
		ODebug.setDebug(true);
		Class<?> c = ct.unwrap(env);
		ODebug.setDebug(false);
		Method m = OTypeUtils.loadMethod(c, "f");
		return m.invoke(null);
	}

	// public void testNull() throws Throwable {
	// OEnv env = new OrigamiContext();
	// assert this.eval(env, new ONullCode(env)) == null;
	// }

	public void testInt() throws Throwable {
		OEnv env = new OrigamiContext();
		assert this.eval(env, env.v(1)).equals(1);
	}

	public void testDouble() throws Throwable {
		OEnv env = new OrigamiContext();
		assert this.eval(env, env.v(1.0)).equals(1.0);
	}

	public void testString() throws Throwable {
		OEnv env = new OrigamiContext();
		assert this.eval(env, env.v("hoge")).equals("hoge");
	}

}
