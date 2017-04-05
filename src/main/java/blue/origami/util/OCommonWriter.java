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

package blue.origami.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;

import blue.nez.ast.LocaleFormat;

public abstract class OCommonWriter {
	protected String fileName = null;
	private PrintStream out;

	public OCommonWriter() {
		this.out = System.out;
		this.lastChar = '\n';
		this.isColor = true;
	}

	/* IO management */

	protected void open(String path) throws IOException {
		this.close();
		if (path != null) {
			this.out = new PrintStream(new FileOutputStream(path));
			this.isColor = false;
		} else {
			this.out = System.out;
			this.isColor = true;
		}
		this.lastChar = '\n';
	}

	public final void close() {
		this.println();
		if (this.out != System.out) {
			this.out.close();
		}
	}

	public final void flush() {
		this.out.flush();
	}

	/* print */

	private char lastChar = '\n';

	public final void print(String text) {
		this.out.print(text);
		if (text.length() > 0) {
			this.lastChar = text.charAt(text.length() - 1);
		}
	}

	public final void println() {
		if (this.lastChar == '\n') {
			return;
		}
		this.out.println();
		this.lastChar = '\n';
	}

	public final void printf(String fmt, Object... args) {
		this.print(StringCombinator.format(fmt, args));
	}

	public final void printf(LocaleFormat fmt, Object... args) {
		this.print(StringCombinator.format(fmt, args));
	}

	public final void println(String fmt, Object... args) {
		this.printf(fmt, args);
		this.println();
	}

	public final void println(LocaleFormat fmt, Object... args) {
		this.printf(fmt, args);
		this.println();
	}

	public final void p(Object text) {
		this.print(text.toString());
	}

	public final void p(String fmt, Object... args) {
		this.printf(fmt, args);
	}

	protected final void pSpace() { // space
		if (this.lastChar == '\n' || this.lastChar == ' ' || this.lastChar == '\t') {
			return;
		}
		this.print(" ");
	}

	/* symbol management */

	private HashMap<String, String> symMap = new HashMap<>();

	protected final String s(String key) {
		return this.s(key, key);
	}

	protected final String s(String key, String def) {
		return this.symMap.getOrDefault(key, def);
	}

	public final void defineSymbol(String key, String s) {
		this.symMap.put(key, s);
	}

	/* color */

	private boolean isColor = true;

	public final String bold(String s) {
		return this.isColor ? OConsole.bold(s) : s;
	}

	public final String red(String s) {
		return this.isColor ? OConsole.color(OConsole.Red, s) : s;
	}

	/* indent management */

	private int IndentLevel = 0;
	private String currentIndentString = "";

	public final void incIndent() {
		this.IndentLevel = this.IndentLevel + 1;
		this.currentIndentString = null;
	}

	public final void decIndent() {
		this.IndentLevel = this.IndentLevel - 1;
		assert (this.IndentLevel >= 0);
		this.currentIndentString = null;
	}

	private final String sIndent() {
		String tab = this.s("\t", "   ");
		if (this.currentIndentString == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < this.IndentLevel; ++i) {
				sb.append(tab);
			}
			this.currentIndentString = sb.toString();
		}
		return this.currentIndentString;
	}

	public final void L(Object text) {
		this.println();
		this.print(this.sIndent());
		this.print(text.toString());
	}

	public final void L(String fmt, Object... args) {
		this.println();
		this.print(this.sIndent());
		this.printf(fmt, args);
	}

	public final void pBegin() {
		this.pBegin(this.s("{"));
	}

	public final void pBegin(String s) {
		this.print(s);
		this.incIndent();
	}

	public final void pEnd() {
		this.pEnd(this.s("}"));
	}

	public final void pEnd(String s) {
		this.decIndent();
		if (!(s == null || s.length() == 0)) {
			this.L(s);
		}
	}

	public void pComment(String fmt, Object... args) {

	}

	public void pVerbose(String fmt, Object... args) {
		this.pComment(fmt, args);
	}

	public final void importFileContent(String path) {
		this.importResourceContent(path);
	}

	public final void importResourceContent(String path, String... stringReplacements) {
		try {
			if (!path.startsWith("/")) {
				path = "/blue/origami/include/" + path;
			}
			InputStream s = OCommonWriter.class.getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(s));
			String line = null;
			while ((line = reader.readLine()) != null) {
				for (int i = 0; i < stringReplacements.length; i += 2) {
					line = line.replace(stringReplacements[i], stringReplacements[i + 1]);
				}
				this.L(line);
			}
			reader.close();
		} catch (Exception e) {
			OConsole.exit(1, e);
		}
	}

	public final void showResourceContent(String path, String... stringReplacements) {
		try {
			InputStream s = OCommonWriter.class.getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(s));
			String line = null;
			while ((line = reader.readLine()) != null) {
				for (int i = 0; i < stringReplacements.length; i += 2) {
					line = line.replace(stringReplacements[i], stringReplacements[i + 1]);
				}
				OConsole.println(line);
			}
			reader.close();
		} catch (Exception e) {
			OConsole.exit(1, e);
		}
	}

}
