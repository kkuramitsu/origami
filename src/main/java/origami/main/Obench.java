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

import origami.nez.ast.Source;
import origami.nez.ast.Tree;
import origami.nez.parser.CommonSource;
import origami.nez.parser.Parser;
import origami.nez.parser.ParserFactory;

public class Obench extends OCommand {
	@Override
	public void exec(ParserFactory factory) throws IOException {
		Parser parser = factory.newParser();
		parser.setThrowingException(false);
		parser.setPrintingException(true);
		if (factory.value("text", null) != null) {
			exit(1, "unavailable -t --text option");
			return;
		}
		double total = 0.0;
		int len = 0;
		String[] files = factory.list("files");
		this.checkInputSource(files);
		for (String file : files) {
			System.out.printf("%s", file);
			double dsum = 0.0;
			for (int c = 0; c < 10; c++) {
				Source input = CommonSource.newFileSource(file, null);
				long t1 = System.nanoTime();
				Tree<?> node = parser.parse(input);
				if (node == null) {
					break;
				}
				long t2 = System.nanoTime();
				double d = (t2 - t1) / 1000000.0;
				if (d < 10.0) {
					break;
				}
			}
			for (int c = 0; c < 5; c++) {
				Source input = CommonSource.newFileSource(file, null);
				long t1 = System.nanoTime();
				Tree<?> node = parser.parse(input);
				if (node == null) {
					System.out.printf("\tFAILED\n", file);
					break;
				}
				long t2 = System.nanoTime();
				double d = (t2 - t1) / 1000000.0;
				System.out.printf("\t%.3f[ms]", d);
				len += input.length();
				dsum += d;
			}
			if (len > 0) {
				System.out.printf("\t(ave) %.3f[ms]\n", (dsum / 5));
				total += dsum;
			}
		}
		double s = (total / 1000);
		p(Blue, "Throughput %.2f [B/s] %.2f [KiB/s] %.2f[MiB/s]", (len / s), (len / 1024 / s), (len / 1024 / 1024 / s));
	}
}
