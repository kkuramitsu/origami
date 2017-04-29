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

import blue.nez.ast.Source;
import blue.nez.parser.Parser;
import blue.nez.parser.ParserSource;
import blue.nez.peg.Grammar;
import blue.nez.peg.SourceGrammar;

public class GrammarTest {

	private Source loadInputText(String file, int num) {
		try {
			return ParserSource.newFileSource("/opeg-test/" + file + "/" + num + ".in", null);
		} catch (IOException e) {
		    return null;
		}
	}

	private void parseExample(String file) throws Throwable {
		Grammar g = SourceGrammar.loadFile("/opeg-test/" + file + ".opeg");
		Parser p = g.newParser();
		for (int i = 0; i < 10; i++) {
			Source sin = this.loadInputText(file, i);
			if (sin == null) {
				break;
			}
			String parsed = "" + p.parse(sin);
			Source s = ParserSource.newFileSource("/opeg-test/" + file + "/1.out", null);
			String result = s.subString(0, s.length());
			if (!result.startsWith(parsed)) {
				System.out.println("FAIL " + file + ", " + i);
				System.out.println("\t" + parsed);
				System.out.println("\t" + result);
                assertThat(result).startsWith(parsed);
			}
		}
	}

	@Test
	public void test_alnum() throws Throwable {
		this.parseExample("alnum");
	}

	@Test
	public void test_recursion() throws Throwable {
		this.parseExample("recursion");
	}

	@Test
	public void test_list() throws Throwable {
		this.parseExample("list");
	}

	@Test
	public void test_lpair() throws Throwable {
		this.parseExample("lpair");
	}

	@Test
	public void test_rpair() throws Throwable {
		this.parseExample("rpair");
	}

	@Test
	public void test_mchar() throws Throwable {
		this.parseExample("mchar");
	}

	@Test
	public void test_math() throws Throwable {
		this.parseExample("math");
	}

	@Test
	public void test_rna() throws Throwable {
		this.parseExample("rna");
	}

	@Test
	public void test_if() throws Throwable {
		this.parseExample("if");
	}

	@Test
	public void test_block() throws Throwable {
		this.parseExample("block");
	}

	@Test
	public void test_local() throws Throwable {
		this.parseExample("local");
	}

	@Test
	public void test_match() throws Throwable {
		this.parseExample("match");
	}

	@Test
	public void test_exists() throws Throwable {
		this.parseExample("exists");
	}

	@Test
	public void test_notexists() throws Throwable {
		this.parseExample("not-exists");
	}

	@Test
	public void test_is() throws Throwable {
		this.parseExample("is");
	}

	@Test
	public void test_notis() throws Throwable {
		this.parseExample("not-is");
	}

	@Test
	public void test_scan() throws Throwable {
		this.parseExample("scan");
	}

}
