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

package blue.origami.nez.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import blue.origami.nez.ast.Source;

public abstract class ParserSource implements Source {

	private String resourceName;
	protected long startLineNum = 1;

	protected ParserSource(String resourceName, long linenum) {
		this.resourceName = resourceName;
		this.startLineNum = linenum;
	}

	@Override
	public final String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Source subSource(long startIndex, long endIndex) {
		return new StringSource(this.getResourceName(), this.linenum(startIndex), this.subBytes(startIndex, endIndex),
				false);
	}

	// @Override
	// public abstract long linenum(long pos);

	@Override
	public final int column(long pos) {
		int count = 0;
		for (long p = pos - 1; p >= 0; p--) {
			if (this.byteAt(p) == '\n') {
				break;
			}
			count++;
		}
		return count;
	}

	/* utils */

	public final static Source newStringSource(String str) {
		return new StringSource(str);
	}

	public final static Source newStringSource(String resource, long linenum, String str) {
		return new StringSource(resource, linenum, str);
	}

	public final static Source newFileSource(String fileName, String[] paths) throws IOException {
		return newFileSource(ParserSource.class, fileName, paths);
	}

	public final static Source newFileSource(Class<?> c, String fileName, String[] paths) throws IOException {
		File f = new File(fileName);
		if (!f.isFile()) {
			if (paths != null) {
				for (String path : paths) {
					path += path.endsWith("/") ? fileName : "/" + fileName;
					InputStream stream = c.getResourceAsStream(path);
					if (stream != null) {
						return newStringSource(fileName, stream);
					}
				}
			} else {
				InputStream stream = c.getResourceAsStream(fileName);
				if (stream != null) {
					return newStringSource(fileName, stream);
				}
			}
		}
		return new FileSource(fileName);
	}

	public static Source newStringSource(String fileName, InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();
		String line = reader.readLine();
		while (true) {
			sb.append(line);
			line = reader.readLine();
			if (line == null) {
				break;
			}
			sb.append("\n");
		}
		reader.close();
		return new StringSource(fileName, 1, sb.toString());
	}
}
