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

package origami.main;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import blue.origami.common.OOption;
import origami.nez2.PEG;
import origami.nez2.ParseTree;
import origami.nez2.Parser;
import origami.nez2.Token;

public class Oexample extends Oparse {
	HashMap<String, Parser> parserMap = new HashMap<>();
	HashMap<String, Long> timeMap = new HashMap<>();

	Coverage cov = null;
	String desc = "";
	int tested = 0;
	int succ = 0;

	@Override
	public void exec(OOption options) throws Throwable {
		String file = pegFile(options);
		PEG peg = new PEG();
		peg.load(file);
		// g.dump();
		if (options.is(MainOption.Coverage, false)) {
			this.cov = new Coverage(peg);
		}
		this.loadExample(file, peg);
		if (this.tested > 0) {
			double passRatio = (double) this.succ / this.tested;
			if (this.cov != null) {
				this.c(Yellow, () -> {
					this.cov.dump();
				});
				double fullcov = this.cov.cov();
				p(this.c(Bold, "Result: %.2f%% passed, %.2f%% (coverage) tested."), (passRatio * 100), (fullcov * 100));
				if (this.tested == this.succ && fullcov > 0.5) {
					p("");
					p(this.c(Bold, "Congratulation!!"));
					p("You are invited to share your grammar at Nez open grammar repository, ");
					p(" http://github.com/nez-peg/grammar.");
					p("If you want, please send a pull-request with:");
					p(this.c(Bold, "git commit -m '" + this.desc + ", %.2f%% (coverage) tested.'"), (fullcov * 100));
				}
			} else {
				p(this.c(Bold, "Result: %.2f%% passed."), (passRatio * 100));
			}
		}
	}

	void loadExample(String file, PEG peg) throws IOException {
		List<ParseTree> list = peg.getMemo("examples");
		if (list != null) {
			for (ParseTree t : list) {
				ParseTree[] ts = t.list();
				Token[] names = Arrays.stream(ts[0].list()).map(x -> x.asToken(file)).toArray(Token[]::new);
				Token doc = ts[1].asToken(file);
				for (int i = 0; i < names.length; i++) {
					Parser p = peg.getParser(names[i].getSymbol());
					if (!this.perform(p, names[i], i, doc)) {
						break;
					}
				}
			}
		}
	}

	protected boolean perform(Parser p, Token name, int count, Token doc) {
		this.tested++;
		try {
			long t1 = System.nanoTime();
			ParseTree t = p.parse(doc);
			this.succ++;
			long t2 = System.nanoTime();
			this.c(Green, () -> {
				p("[PASS] " + name);
			});
			if (count == 0) {
				p("   " + t);
			}
			this.record(name.getSymbol(), t2 - t1);
			return true;
		} catch (IOException e) {
			this.c(Red, () -> {
				p("[FAIL] " + name);
				System.err.println(doc);
				p("===");
				e.printStackTrace();
			});
		}
		return false;
	}

	private void record(String uname, long t) {
		Long l = this.timeMap.get(uname);
		if (l == null) {
			l = t;
		} else {
			l += t;
		}
		this.timeMap.put(uname, l);
	}

}