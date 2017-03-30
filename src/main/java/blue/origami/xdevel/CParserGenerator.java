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

package blue.origami.xdevel;

import blue.origami.nez.peg.Grammar;
import blue.origami.util.OStringUtils;

public class CParserGenerator extends ParserGenerator {

	public CParserGenerator() {
		// this.fileBase = "cnez";
		// super(".c");
	}

	@Override
	protected void initLanguageSpec() {
		this.SupportedRange = true;
		this.SupportedMatch2 = true;
		this.SupportedMatch3 = true;
		this.SupportedMatch4 = true;
		this.SupportedMatch5 = true;
		this.SupportedMatch6 = true;
		this.SupportedMatch7 = true;
		this.SupportedMatch8 = true;

		this.addType("$parse", "int");
		this.addType("$tag", "int");
		this.addType("$label", "int");
		this.addType("$table", "int");
		this.addType("$arity", "int");
		this.addType("$text", "const unsigned char");
		this.addType("$index", "const unsigned char");
		if (this.UsingBitmap) {
			this.addType("$set", "int");
		} else {
			this.addType("$set", "const unsigned char");
		}
		this.addType("$range", "const unsigned char __attribute__((aligned(16)))");
		this.addType("$string", "const char *");

		this.addType("memo", "int");
		if (this.UsingBitmap) {
			this.addType(this._set(), "int");
		} else {
			this.addType(this._set(), "const unsigned char *");/* boolean */
		}
		this.addType(this._index(), "const unsigned char *");
		this.addType(this._temp(), "int");/* boolean */
		this.addType(this._pos(), "const unsigned char *");
		this.addType(this._tree(), "size_t");
		this.addType(this._log(), "size_t");
		this.addType(this._table(), "size_t");
		this.addType(this._state(), "ParserContext *");
	}

	@Override
	protected String _True() {
		return "1";
	}

	@Override
	protected String _False() {
		return "0";
	}

	@Override
	protected String _Null() {
		return "NULL";
	}

	/* Expression */

	@Override
	protected String _Field(String o, String name) {
		return o + "->" + name;
	}

	@Override
	protected String _Func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append("ParserContext_");
		sb.append(name);
		sb.append("(");
		sb.append(this._state());
		for (int i = 0; i < args.length; i++) {
			sb.append(",");
			sb.append(args[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String _text(byte[] text) {
		return super._text(text) + ", " + this._int(text.length);
	}

	@Override
	protected String _text(String key) {
		if (key == null) {
			return this._Null() + ", 0";
		}
		return this.nameMap.get(key) + ", " + this._int(OStringUtils.utf8(key).length);
	}

	@Override
	protected String _defun(String type, String name) {
		if (this.crossRefNames.contains(name)) {
			return type + " " + name;
		}
		return "static inline " + type + " " + this._rename(name);
	}

	@Override
	protected String _rename(String name) {
		char[] l = name.toCharArray();
		StringBuilder result = new StringBuilder();
		for (char ch : l) {
			if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9') || ch == '_') {
				result.append(ch);
				continue;
			}
			result.append((int) ch);
		}
		return result.toString();
	}

	/* Statement */

	@Override
	protected void DeclConst(String type, String name, String expr) {
		this.Statement("static " + type + " " + name + " = " + expr);
	}

	// Grammar Generator

	@Override
	protected void generateHeader(Grammar g) {
		this.importFileContent("cnez-runtime.txt");
	}

	@Override
	protected void generatePrototypes() {
		this.pComment("Prototypes");
		for (String name : this.crossRefNames) {
			this.Statement(this._defun("int", name) + "(ParserContext *c)");
		}
	}

	@Override
	protected void generateFooter(Grammar g) {
		this.importFileContent("cnez-utils.txt");
		//
		this.BeginDecl("void* " + this._ns()
				+ "parse(const char *text, size_t len, void *thunk, void* (*fnew)(symbol_t, const unsigned char *, size_t, size_t, void *), void  (*fset)(void *, size_t, symbol_t, void *, void *), void  (*fgc)(void *, int, void *))");
		{
			this.VarDecl("void*", "result", this._Null());
			this.VarDecl(this._state(), "ParserContext_new((const unsigned char*)text, len)");
			this.Statement(this._Func("initTreeFunc", "thunk", "fnew", "fset", "fgc"));
			this.InitMemoPoint();
			this.If(this._funccall(this._funcname(g.getStartProduction())));
			{
				this.VarAssign("result", this._Field(this._state(), this._tree()));
				this.If("result == NULL");
				{
					this.Statement(
							"result = c->fnew(0, (const unsigned char*)text, (c->pos - (const unsigned char*)text), 0, c->thunk)");
				}
				this.EndIf();
			}
			this.EndIf();
			this.Statement(this._Func("free"));
			this.Return("result");
		}
		this.EndDecl();
		this.BeginDecl("static void* cnez_parse(const char *text, size_t len)");
		{
			this.Return(this._ns() + "parse(text, len, NULL, NULL, NULL, NULL)");
		}
		this.EndDecl();
		this.BeginDecl("long " + this._ns() + "match(const char *text, size_t len)");
		{
			this.VarDecl("long", "result", "-1");
			this.VarDecl(this._state(), "ParserContext_new((const unsigned char*)text, len)");
			this.Statement(this._Func("initNoTreeFunc"));
			this.InitMemoPoint();
			this.If(this._funccall(this._funcname(g.getStartProduction())));
			{
				this.VarAssign("result", this._cpos() + "-" + this._Field(this._state(), "inputs"));
			}
			this.EndIf();
			this.Statement(this._Func("free"));
			this.Return("result");
		}
		this.EndDecl();
		this.BeginDecl("const char* " + this._ns() + "tag(symbol_t n)");
		{
			this.Return("_tags[n]");
		}
		this.EndDecl();
		this.BeginDecl("const char* " + this._ns() + "label(symbol_t n)");
		{
			this.Return("_labels[n]");
		}
		this.EndDecl();
		this.L("#ifndef UNUSE_MAIN");
		this.BeginDecl("int main(int ac, const char **argv)");
		{
			this.Return("cnez_main(ac, argv, cnez_parse)");
		}
		this.EndDecl();
		this.L("#endif/*MAIN*/");
		this.L("// End of File");
		// generateHeaderFile();
		// this.showManual("cnez-man.txt", new String[] { "$cmd$", _basename()
		// });
	}

	private void generateHeaderFile() {
		// FIXME : this.setFileBuilder(".h");
		this.Statement("typedef unsigned long int symbol_t");
		int c = 1;
		for (String s : this.tagList) {
			if (s.equals("")) {
				continue;
			}
			this.L("#define _" + s + " ((symbol_t)" + c + ")");
			c++;
		}
		this.L("#define MAXTAG " + c);
		c = 1;
		for (String s : this.labelList) {
			if (s.equals("")) {
				continue;
			}
			this.L("#define _" + s + " ((symbol_t)" + c + ")");
			c++;
		}
		this.L("#define MAXLABEL " + c);
		this.Statement("void* " + this._ns()
				+ "parse(const char *text, size_t len, void *, void* (*fnew)(symbol_t, const char *, size_t, size_t, void *), void  (*fset)(void *, size_t, symbol_t, void *, void *), void  (*fgc)(void *, int, void *))");
		this.Statement("long " + this._ns() + "match(const char *text, size_t len)");
		this.Statement("const char* " + this._ns() + "tag(symbol_t n)");
		this.Statement("const char* " + this._ns() + "label(symbol_t n)");
		this.close();
	}

}
