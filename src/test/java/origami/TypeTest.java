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

import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.SourceGrammar;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.OConsole;

public class TypeTest {

	public void testLiteral() throws Throwable {
		runScript("()", "()");
		runScript("true", "Bool");
		runScript("false", "Bool");
		runScript("1", "Int");
		runScript("1.0", "Float");
		runScript("'a'", "Char");
		runScript("'abc'", "String");
		runScript("\"abc\"", "String");
		runScript("[1,2,3]", "Int[]");
		runScript("{1,2,3}", "Int{}");
	}

	public void testBinary() throws Throwable {
		runScript("1+1.0", "Float");
		runScript("1.0+1", "Float");
	}

	public void testLet() throws Throwable {
		runScript("a = 1\na", "Int");
	}

	public void testLambda() throws Throwable {
		runScript("\\n n+1", "Int->Int");
		runScript("\\() 1", "()->Int");
	}

	public void testTemplate() throws Throwable {
		runScript("f(a)=|a|;f", "a->Int");
		runScript("f(a)=|a|;f(1);f", "Int->Int");
	}

	public void testAdHoc() throws Throwable {
		runScript2("f(a)=2a+1;f", "a->b");
		runScript("f(a)=2a+1;f(1);f", "Int->Int");
		runScript("f(a)=2a+1;f(1.0);f", "Float->Float");
	}

	public void testOption() throws Throwable {
		runScript("Some(1)", "Option[Int]");
		runScript("Some(1) >>= (\\n Some(n+1))", "Option[Int]");
	}

	public void testData() throws Throwable {
		runScript("f(p) = p.x + p.y; f", "[x,y]->Float");
	}

	public void testMutation() throws Throwable {
		runScript2("f(a,b)=a[0]+b[0];f", "(a[],a[])->a");
		runScript2("f()={1,2};f", "()->Int[]");
		runScript2("f(a)=a[0]=1;f", "Int{}->()");
	}

	//
	public static void runScript(String text, String checked) throws Throwable {
		Grammar g = SourceGrammar.loadFile("/blue/origami/grammar/konoha5.opeg");
		Transpiler env = new Transpiler(g, "jvm");
		Ty ty = env.testType(text);
		System.out.printf("%s %s :: %s\n", TFmt.Checked, text, ty);
		if (checked != null) {
			assert (checked.equals(ty.toString())) : ty + " != " + checked;
		}
	}

	public static void runScript2(String text, String checked) throws Throwable {
		Grammar g = SourceGrammar.loadFile("/blue/origami/grammar/konoha5.opeg");
		Transpiler env = new Transpiler(g, "jvm");
		Ty ty = env.testType(text);
		if (checked.equals(ty.toString())) {
			System.out.printf("%s %s :: %s\n", TFmt.Checked, text, ty);
		} else {
			System.out.printf(OConsole.color(OConsole.Red, "%s %s :: %s \n"), TFmt.Checked, text, ty);
		}
	}

}
