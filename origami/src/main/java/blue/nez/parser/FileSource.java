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

package blue.nez.parser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import blue.origami.util.OConsole;

public class FileSource extends ParserSource {
	public final static int PageSize = 4096;

	private RandomAccessFile file;
	private long fileLength = 0;
	private long buffer_offset;
	private byte[] buffer;
	private long lines[];

	private final int FifoSize = 8;
	private LinkedHashMap<Long, byte[]> fifoMap = null;

	public FileSource(String fileName) throws IOException {
		super(fileName, 1);
		try {
			this.file = new RandomAccessFile(fileName, "r");
			this.fileLength = this.file.length();

			this.buffer_offset = 0;
			lines = new long[((int) this.fileLength / PageSize) + 1];
			lines[0] = 1;
			if (this.FifoSize > 0) {
				this.fifoMap = new LinkedHashMap<Long, byte[]>(FifoSize) { // FIFO
					private static final long serialVersionUID = 6725894996600788028L;

					@Override
					protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
						if (this.size() > FifoSize) {
							return true;
						}
						return false;
					}
				};
				this.buffer = null;
			} else {
				this.fifoMap = null;
				this.buffer = new byte[PageSize];
			}
			this.readMainBuffer(this.buffer_offset);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public final long length() {
		return this.fileLength;
	}

	private long buffer_alignment(long pos) {
		return (pos / PageSize) * PageSize;
	}

	@Override
	public final int byteAt(long pos) {
		int buffer_pos = (int) (pos - this.buffer_offset);
		if (!(buffer_pos >= 0 && buffer_pos < PageSize)) {
			this.buffer_offset = buffer_alignment(pos);
			this.readMainBuffer(this.buffer_offset);
			buffer_pos = (int) (pos - this.buffer_offset);
		}
		return this.buffer[buffer_pos] & 0xff;
	}

	@Override
	public final boolean eof(long pos) {
		return pos >= this.length(); //
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		int offset = (int) (pos - this.buffer_offset);
		if (offset >= 0 && offset + text.length <= PageSize) {
			switch (text.length) {
			case 0:
				break;
			case 1:
				if (text[0] == this.buffer[offset]) {
					return true;
				}
				return false;
			case 2:
				if (text[0] == this.buffer[offset] && text[1] == this.buffer[offset + 1]) {
					return true;
				}
				return false;
			case 3:
				if (text[0] == this.buffer[offset] && text[1] == this.buffer[offset + 1]
						&& text[2] == this.buffer[offset + 2]) {
					return true;
				}
				return false;
			case 4:
				if (text[0] == this.buffer[offset] && text[1] == this.buffer[offset + 1]
						&& text[2] == this.buffer[offset + 2] && text[3] == this.buffer[offset + 3]) {
					return true;
				}
				return false;
			default:
				for (int i = 0; i < text.length; i++) {
					if (text[i] != this.buffer[offset + i]) {
						return false;
					}
				}
			}
			return true;
		}
		for (int i = 0; i < text.length; i++) {
			if ((text[i] & 0xff) != this.byteAt(pos + i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String subString(long startIndex, long endIndex) {
		if (endIndex > startIndex) {
			try {
				long off_s = buffer_alignment(startIndex);
				long off_e = buffer_alignment(endIndex);
				if (off_s == off_e) {
					if (this.buffer_offset != off_s) {
						this.buffer_offset = off_s;
						this.readMainBuffer(this.buffer_offset);
					}
					return new String(this.buffer, (int) (startIndex - this.buffer_offset),
							(int) (endIndex - startIndex), "UTF8");
				} else {
					byte[] b = new byte[(int) (endIndex - startIndex)];
					this.readStringBuffer(startIndex, b);
					return new String(b, "UTF8");
				}
			} catch (UnsupportedEncodingException e) {
				OConsole.exit(1, e);
			}
		}
		return "";
	}

	@Override
	public final byte[] subBytes(long startIndex, long endIndex) {
		byte[] b = null;
		if (endIndex > startIndex) {
			long off_s = buffer_alignment(startIndex);
			long off_e = buffer_alignment(endIndex);
			b = new byte[(int) (endIndex - startIndex)];
			if (off_s == off_e) {
				if (this.buffer_offset != off_s) {
					this.buffer_offset = off_s;
					this.readMainBuffer(this.buffer_offset);
				}
				System.arraycopy(this.buffer, (int) (startIndex - this.buffer_offset), b, 0, b.length);
			} else {
				this.readStringBuffer(startIndex, b);
			}
		}
		return b;
	}

	private int lineIndex(long pos) {
		return (int) (pos / PageSize);
	}

	private long startLineNum(long pos) {
		int index = lineIndex(pos);
		return this.lines[index];
	}

	@Override
	public final long linenum(long pos) {
		long count = startLineNum(pos);
		byteAt(pos); // restore buffer at pos
		int offset = (int) (pos - this.buffer_offset);
		for (int i = 0; i < offset; i++) {
			if (this.buffer[i] == '\n') {
				count++;
			}
		}
		return count;
	}

	private void readMainBuffer(long pos) {
		int index = lineIndex(pos);
		if (this.lines[index] == 0) {
			long count = this.lines[index - 1];
			for (int i = 0; i < this.buffer.length; i++) {
				if (this.buffer[i] == '\n') {
					count++;
				}
			}
			this.lines[index] = count;
		}
		if (this.fifoMap != null) {
			Long key = pos;
			byte[] buf = this.fifoMap.get(key);
			if (buf == null) {
				buf = new byte[PageSize];
				this.readBuffer(pos, buf);
				this.fifoMap.put(key, buf);
				this.buffer = buf;
			} else {
				this.buffer = buf;
			}
		} else {
			this.readBuffer(pos, this.buffer);
		}
	}

	private void readBuffer(long pos, byte[] b) {
		try {
			this.file.seek(pos);
			int readsize = this.file.read(b);
			for (int i = readsize; i < b.length; i++) {
				b[i] = 0;
			}
		} catch (IOException e) {
			// ODebug.traceException(e);
		}
	}

	private void readStringBuffer(long pos, byte[] buf) {
		if (this.fifoMap != null) {
			int copied = 0;
			long start = pos;
			long end = pos + buf.length;
			while (start < end) {
				long offset = this.buffer_alignment(start);
				if (this.buffer_offset != offset) {
					this.buffer_offset = offset;
					this.readMainBuffer(offset);
				}
				int start_off = (int) (start - offset);
				int end_off = (int) (end - offset);
				if (end_off <= PageSize) {
					int len = end_off - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					assert (copied == buf.length);
					return;
				} else {
					int len = PageSize - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					start += len;
				}
			}
		} else {
			this.readBuffer(pos, buf);
		}
	}

}