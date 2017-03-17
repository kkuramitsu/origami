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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import origami.nez.ast.Source;
import origami.trait.OStringUtils;

public abstract class CommonSource implements Source {

	private String resourceName;
	protected long startLineNum = 1;

	protected CommonSource(String resourceName, long linenum) {
		this.resourceName = resourceName;
		this.startLineNum = linenum;
	}

	@Override
	public final String getResourceName() {
		return resourceName;
	}

	@Override
	public abstract long length();

	@Override
	public abstract int byteAt(long pos);

	@Override
	public abstract boolean eof(long pos);

	@Override
	public abstract boolean match(long pos, byte[] text);

	@Override
	public abstract String subString(long startIndex, long endIndex);

	@Override
	public Source subSource(long startIndex, long endIndex) {
		return new StringSource(this.getResourceName(), this.linenum(startIndex), subByte(startIndex, endIndex), false);
	}

	@Override
	public abstract long linenum(long pos);

	@Override
	public final int column(long pos) {
		int count = 0;
		for (long p = pos - 1; p >= 0; p--) {
			if (this.byteAt(pos) == '\n') {
				break;
			}
			count++;
		}
		return count;
	}

	@Override
	public final String getLineString(long pos) {
		long start = pos - 1;
		for (; start >= 0; start--) {
			if (this.byteAt(start) == '\n') {
				break;
			}
		}
		long end = start;
		for (; this.byteAt(end) != 0; end++) {
			if (this.byteAt(end) == '\n') {
				break;
			}
		}
		return subString(start, end);
	}

	/* handling input stream */

	// final String getFilePath(String fileName) {
	// int loc = this.getResourceName().lastIndexOf("/");
	// if(loc > 0) {
	// return this.getResourceName().substring(0, loc+1) + fileName;
	// }
	// return fileName;
	// }

	private final long getLineStartPosition(long fromPostion) {
		long startIndex = fromPostion;
		if (!(startIndex < this.length())) {
			startIndex = this.length() - 1;
		}
		if (startIndex < 0) {
			startIndex = 0;
		}
		while (startIndex > 0) {
			int ch = byteAt(startIndex);
			if (ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getIndentText(long fromPosition) {
		long startPosition = this.getLineStartPosition(fromPosition);
		long i = startPosition;
		String indent = "";
		for (; i < fromPosition; i++) {
			int ch = this.byteAt(i);
			if (ch != ' ' && ch != '\t') {
				if (i + 1 != fromPosition) {
					for (long j = i; j < fromPosition; j++) {
						indent = indent + " ";
					}
				}
				break;
			}
		}
		indent = this.subString(startPosition, i) + indent;
		return indent;
	}

	public final String formatPositionMessage(String messageType, long pos, String message) {
		return "(" + extractFileName(this.getResourceName()) + ":" + this.linenum(pos) + ") [" + messageType + "] " + message;
	}

	public final static String extractFileName(String path) {
		int loc = path.lastIndexOf('/');
		if (loc > 0) {
			return path.substring(loc + 1);
		}
		loc = path.lastIndexOf('\\');
		if (loc > 0) {
			return path.substring(loc + 1);
		}
		return path;
	}

	public final static String extractFileExtension(String path) {
		int loc = path.lastIndexOf('.');
		if (loc > 0) {
			return path.substring(loc + 1);
		}
		return path;
	}

	@Override
	public final String formatPositionLine(String messageType, long pos, String message) {
		return this.formatPositionMessage(messageType, pos, message) + this.getTextAround(pos, "\n ");
	}

	private final String getTextAround(long pos, String delim) {
		int ch = 0;
		if (pos < 0) {
			pos = 0;
		}
		while ((this.eof(pos) || this.byteAt(pos) == 0) && pos > 0) {
			pos -= 1;
		}
		long startIndex = pos;
		while (startIndex > 0) {
			ch = byteAt(startIndex);
			if (ch == '\n' && pos - startIndex > 0) {
				startIndex = startIndex + 1;
				break;
			}
			if (pos - startIndex > 60 && ch < 128) {
				break;
			}
			startIndex = startIndex - 1;
		}
		long endIndex = pos + 1;
		if (endIndex < this.length()) {
			while ((ch = byteAt(endIndex)) != 0 /* this.EOF() */) {
				if (ch == '\n' || endIndex - startIndex > 78 && ch < 128) {
					break;
				}
				endIndex = endIndex + 1;
			}
		} else {
			endIndex = this.length();
		}
		ArrayList<Byte> source = new ArrayList<>();
		// ByteBuffer source = new ByteBuffer();
		// ByteBuilder source = new StringBuilder();
		StringBuilder marker = new StringBuilder();
		for (long i = startIndex; i < endIndex; i++) {
			ch = byteAt(i);
			if (ch == '\n') {
				// source.append("\\n");
				source.add((byte) '\\');
				source.add((byte) '\n');
				if (i == pos) {
					marker.append("^^");
				} else {
					marker.append("\\n");
				}
			} else if (ch == '\t') {
				// source.append(" ");
				source.add((byte) ' ');
				source.add((byte) ' ');
				source.add((byte) ' ');
				source.add((byte) ' ');
				if (i == pos) {
					marker.append("^^^^");
				} else {
					marker.append("    ");
				}
			} else {
				// source.append((char) ch);
				source.add((byte) ch);
				if (i == pos) {
					marker.append("^");
				} else {
					marker.append(" ");
				}
			}
		}
		byte[] b = new byte[source.size()];
		for (int i = 0; i < b.length; i++) {
			b[i] = source.get(i);
		}
		return delim + OStringUtils.newString(b) + delim + marker.toString();
	}

	public final static Source newStringSource(String str) {
		return new StringSource(str);
	}

	public final static Source newStringSource(String resource, long linenum, String str) {
		return new StringSource(resource, linenum, str);
	}

	public final static Source newFileSource(String fileName, String[] paths) throws IOException {
		File f = new File(fileName);
		if (!f.isFile() && paths != null) {
			for (String path : paths) {
				if (path.endsWith("/")) {
					path += fileName;
				} else {
					path += "/" + fileName;
				}
				InputStream stream = CommonSource.class.getResourceAsStream(path);
				if (stream != null) {
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
		}
		return new FileSource(fileName);
	}
}
