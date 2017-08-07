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

import blue.origami.lang.OEnv;
import blue.origami.ocode.OCode;
import blue.origami.rule.ScriptAnalysis;

public class AsmTest {

	public Object eval(OEnv env, OCode... codes) throws Throwable {
		return ScriptAnalysis.eval(env, codes);
	}

	// public void testNull() throws Throwable {
	// OEnv env = new OrigamiContext();
	// assert this.eval(env, new NullCode(env)) == null;
	// }
	//
	// public void testBool() throws Throwable {
	// OEnv env = new OrigamiContext();
	// assert this.eval(env, env.v(true)).equals(true);
	// }
	//
	// public void testInt() throws Throwable {
	// OEnv env = new OrigamiContext();
	// assert this.eval(env, env.v(1)).equals(1);
	// }
	//
	// public void testDouble() throws Throwable {
	// OEnv env = new OrigamiContext();
	// assert this.eval(env, env.v(1.0)).equals(1.0);
	// }
	//
	// public void testString() throws Throwable {
	// OEnv env = new OrigamiContext();
	// assert this.eval(env, env.v("hoge")).equals("hoge");
	// }
	//
	// public void testLocalVariable() throws Throwable {
	// OEnv env = new OrigamiContext();
	// AssignCode body = new AssignCode(true, "v", env.v(1));
	// assert this.eval(env, body).equals(1);
	// }
	//
	// public void testLocalAssign() throws Throwable {
	// OEnv env = new OrigamiContext();
	// AssignCode body = new AssignCode(true, "v", env.v(1));
	// assert this.eval(env, body, new NameCode("v",
	// env.t(int.class))).equals(1);
	// }
	//
	// public void testIf() throws Throwable {
	// OEnv env = new OrigamiContext();
	// IfCode body = new IfCode(env, env.v(true), env.v(1), env.v(2));
	// assert this.eval(env, body).equals(1);
	// }
	//
	// public void testIfElse() throws Throwable {
	// OEnv env = new OrigamiContext();
	// IfCode body = new IfCode(env, env.v(false), env.v(2), env.v(1));
	// assert this.eval(env, body).equals(1);
	// }
	//
	// public void testTry() throws Throwable {
	// OEnv env = new OrigamiContext();
	// TryCode body = new TryCode(env, env.v(1), new EmptyCode(env));
	// assert this.eval(env, body).equals(1);
	// }
	//
	// public void testTryFinally() throws Throwable {
	// OEnv env = new OrigamiContext();
	// OCode init = new AssignCode(true, "v", env.v(1)).asType(env,
	// env.t(void.class));
	// OCode finl = new AssignCode(false, "v", env.v(2)).asType(env,
	// env.t(void.class));
	// TryCode body = new TryCode(env, env.v(2), finl);
	// assert this.eval(env, init, body).equals(2);
	// }
	//
	// public void testTryCatchFinally() throws Throwable {
	// OEnv env = new OrigamiContext();
	// OCode init = new AssignCode(true, "v", env.v(1)).asType(env,
	// env.t(void.class));
	// OCode finl = new AssignCode(false, "v", env.v(2)).asType(env,
	// env.t(void.class));
	// TryCode.CatchCode c = new TryCode.CatchCode(env.t(IOException.class),
	// "e", env.v(2));
	//
	// TryCode body = new TryCode(env, env.v(2), finl, c);
	// assert this.eval(env, init, body).equals(2);
	// }
	//
	// public void testTryCatch() throws Throwable {
	// OEnv env = new OrigamiContext();
	// TryCode.CatchCode c = new TryCode.CatchCode(env.t(IOException.class),
	// "e", env.v(2));
	// TryCode body = new TryCode(env, env.v(1), new EmptyCode(env), c);
	// assert this.eval(env, body).equals(1);
	// }
	//
	// public void testTryCatch2() throws Throwable {
	// OEnv env = new OrigamiContext();
	// TryCode.CatchCode c = new TryCode.CatchCode(env.t(IOException.class),
	// "e", env.v(2));
	// TryCode.CatchCode c2 = new
	// TryCode.CatchCode(env.t(NullPointerException.class), "e", env.v(2));
	// TryCode body = new TryCode(env, env.v(1), new EmptyCode(env), c, c2);
	// assert this.eval(env, body).equals(1);
	// }

}
