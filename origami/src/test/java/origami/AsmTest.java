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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import blue.origami.OrigamiContext;
import blue.origami.lang.OEnv;
import blue.origami.ocode.AssignCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.ocode.IfCode;
import blue.origami.ocode.NameCode;
import blue.origami.ocode.NullCode;
import blue.origami.ocode.TryCode;
import blue.origami.rule.ScriptAnalysis;

public class AsmTest {

	private Object eval(OEnv env, OCode... codes) throws Throwable {
		return ScriptAnalysis.eval(env, codes);
	}

	@Test
	public void testNull() throws Throwable {
		OEnv env = new OrigamiContext();
		assertThat(eval(env, new NullCode(env))).isNull();
	}

	@Test
	public void testBool() throws Throwable {
		OEnv env = new OrigamiContext();
		assertThat(eval(env, env.v(true))).isEqualTo(true);
	}

	@Test
	public void testInt() throws Throwable {
		OEnv env = new OrigamiContext();
		assertThat(eval(env, env.v(1))).isEqualTo(1);
	}

	@Test
	public void testDouble() throws Throwable {
		OEnv env = new OrigamiContext();
		assertThat(eval(env, env.v(1.0))).isEqualTo(1.0);
	}

	@Test
	public void testString() throws Throwable {
		OEnv env = new OrigamiContext();
		assertThat(eval(env, env.v("hoge"))).isEqualTo("hoge");
	}

	@Test
	public void testLocalVariable() throws Throwable {
		OEnv env = new OrigamiContext();
		AssignCode body = new AssignCode(true, "v", env.v(1));
		assertThat(eval(env, body)).isEqualTo(1);
	}

	@Test
	public void testLocalAssign() throws Throwable {
		OEnv env = new OrigamiContext();
		AssignCode body = new AssignCode(true, "v", env.v(1));
		assertThat(eval(env, body, new NameCode("v", env.t(int.class)))).isEqualTo(1);
	}

	@Test
	public void testIf() throws Throwable {
		OEnv env = new OrigamiContext();
		IfCode body = new IfCode(env, env.v(true), env.v(1), env.v(2));
		assertThat(eval(env, body)).isEqualTo(1);
	}

	@Test
	public void testIfElse() throws Throwable {
		OEnv env = new OrigamiContext();
		IfCode body = new IfCode(env, env.v(false), env.v(2), env.v(1));
		assertThat(eval(env, body)).isEqualTo(1);
	}

	@Test
	public void testTry() throws Throwable {
		OEnv env = new OrigamiContext();
		TryCode body = new TryCode(env, env.v(1), new EmptyCode(env));
		assertThat(eval(env, body)).isEqualTo(1);
	}

	@Test
	public void testTryFinally() throws Throwable {
		OEnv env = new OrigamiContext();
		OCode init = new AssignCode(true, "v", env.v(1)).asType(env, env.t(void.class));
		OCode finl = new AssignCode(false, "v", env.v(2)).asType(env, env.t(void.class));
		TryCode body = new TryCode(env, env.v(2), finl);
		assertThat(eval(env, init, body)).isEqualTo(2);
	}

	@Test
	public void testTryCatchFinally() throws Throwable {
		OEnv env = new OrigamiContext();
		OCode init = new AssignCode(true, "v", env.v(1)).asType(env, env.t(void.class));
		OCode finl = new AssignCode(false, "v", env.v(2)).asType(env, env.t(void.class));
		TryCode.CatchCode c = new TryCode.CatchCode(env.t(IOException.class), "e", env.v(2));

		TryCode body = new TryCode(env, env.v(2), finl, c);
		assertThat(eval(env, init, body)).isEqualTo(2);
	}

	@Test
	public void testTryCatch() throws Throwable {
		OEnv env = new OrigamiContext();
		TryCode.CatchCode c = new TryCode.CatchCode(env.t(IOException.class), "e", env.v(2));
		TryCode body = new TryCode(env, env.v(1), new EmptyCode(env), c);
		assertThat(eval(env, body)).isEqualTo(1);
	}

	@Test
	public void testTryCatch2() throws Throwable {
		OEnv env = new OrigamiContext();
		TryCode.CatchCode c = new TryCode.CatchCode(env.t(IOException.class), "e", env.v(2));
		TryCode.CatchCode c2 = new TryCode.CatchCode(env.t(NullPointerException.class), "e", env.v(2));
		TryCode body = new TryCode(env, env.v(1), new EmptyCode(env), c, c2);
		assertThat(eval(env, body)).isEqualTo(1);
	}

}
