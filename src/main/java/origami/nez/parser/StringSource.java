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

package origami.nez.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import origami.nez.ast.Source;
import origami.trait.OStringUtils;

public class StringSource extends CommonSource {
	public final byte[] inputs;
	long length;

	public StringSource(String sourceText) {
		super("(string)", 1);
		this.inputs = toZeroTerminalByteSequence(sourceText);
		this.length = inputs.length - 1;
	}

	public StringSource(String resource, long linenum, String sourceText) {
		super(resource, linenum);
		this.inputs = toZeroTerminalByteSequence(sourceText);
		this.length = inputs.length - 1;
	}

	public StringSource(String resource, long linenum, byte[] buffer, boolean nullChar) {
		super(resource, linenum);
		if (nullChar) {
			this.inputs = buffer;
			this.length = buffer.length - 1;
		} else {
			this.inputs = new byte[buffer.length + 1];
			System.arraycopy(buffer, 0, this.inputs, 0, buffer.length);
		}
		this.length = inputs.length - 1;
	}

	private final byte[] toZeroTerminalByteSequence(String s) {
		byte[] b = OStringUtils.utf8(s);
		byte[] b2 = new byte[b.length + 1];
		System.arraycopy(b, 0, b2, 0, b.length);
		return b2;
	}

	@Override
	public final long length() {
		return this.length;
	}

	@Override
	public final int byteAt(long pos) {
		return this.inputs[(int) pos] & 0xff;
	}

	@Override
	public final boolean eof(long pos) {
		return pos >= this.length;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		if (pos + text.length > this.length) {
			return false;
		}
		for (int i = 0; i < text.length; i++) {
			if (text[i] != this.inputs[(int) pos + i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final byte[] subByte(long startIndex, long endIndex) {
		byte[] b = new byte[(int) (endIndex - startIndex)];
		System.arraycopy(this.inputs, (int) (startIndex), b, 0, b.length);
		return b;
	}

	@Override
	public final String subString(long startIndex, long endIndex) {
		try {
			return new String(this.inputs, (int) (startIndex), (int) (endIndex - startIndex), OStringUtils.DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	@Override
	public Source subSource(long startIndex, long endIndex) {
		byte[] b = new byte[(int) (endIndex - startIndex) + 1];
		System.arraycopy(this.inputs, (int) (startIndex), b, 0, b.length);
		return new StringSource(this.getResourceName(), this.linenum(startIndex), b, true);
	}

	@Override
	public final long linenum(long pos) {
		long count = this.startLineNum;
		int end = (int) pos;
		if (end >= this.inputs.length) {
			end = this.inputs.length;
		}
		for (int i = 0; i < end; i++) {
			if (this.inputs[i] == '\n') {
				count++;
			}
		}
		return count;
	}

	/* utils */

	public final static CommonSource loadClassPath(String fileName, String[] classPath) throws IOException {
		File f = new File(fileName);
		if (f.isFile()) {
			return loadStream(f.getAbsolutePath(), new FileInputStream(f));
		}
		for (String path : classPath) {
			path = "/" + path + "/" + fileName;
			InputStream stream = CommonSource.class.getResourceAsStream(path);
			if (stream != null) {
				return loadStream(path, stream);
			}
		}
		throw new FileNotFoundException(fileName);
	}

	private final static CommonSource loadStream(String urn, InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (true) {
			builder.append(line);
			line = reader.readLine();
			if (line == null) {
				break;
			}
			builder.append("\n");
		}
		reader.close();
		return new StringSource(urn, 1, builder.toString());
	}

}