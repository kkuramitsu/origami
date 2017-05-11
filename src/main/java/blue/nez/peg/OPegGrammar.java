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

package blue.nez.peg;

public class OPegGrammar extends ParserCombinator {

	public Expression pStart() {
		return this.Expr("@_", this.Choice("@Expression", "@Source"), "@EOT");
	}

	public Expression pSource() {
		return this.Expr("@_", this.Tree(this.ZeroMore(this.Link(null, "@Stmt")), "#Source"));
	}

	public Expression p_() {
		return this.ZeroMore(this.Choice("@S", "@COMMENT"));
	}

	public Expression pS() {
		return this.Choice(//
				this.Range(' ', ' ', '\t', '\t', '\r', '\r', '\n', '\n'), //
				this.S("\u3000")//
		);
	}

	public Expression pCOMMENT() {
		return this.Choice(//
				this.Expr(this.S("/*"), this.ZeroMore(this.Not(this.S("*/")), this.AnyChar()), this.S("*/")), //
				this.Expr(this.S("//"), this.ZeroMore(this.Not(("@EOL")), this.AnyChar()), ("@EOL")), //
				this.Expr(this.S("format"), "@S", this.S("#"), this.ZeroMore(this.NotAny('\n')))//
		);
	}

	public Expression pEOL() {
		return this.Choice("'\n'", this.Expr("'\r'", "'\n'"), "@EOT");
	}

	public Expression pEOT() {
		return this.Not(this.AnyChar());
	}

	public Expression pStmt() {
		return this.Expr(this.Choice("@Grammar", "@Import", "@Example", "@Production"), "@_",
				this.Option(this.S(";"), "@_"));
	}

	public Expression pGrammar() {
		return this.Tree(this.S("grammar"), ("@_"), //
				this.Link("$name", "@Name"), ("@_"), //
				this.S("{"), ("@_"), //
				this.Link("$body", "@Source"), //
				this.S("}"), //
				"#Grammar"//
		);
	}

	public Expression pKEYWORD() {
		return this.Expr(this.Choice(//
				this.S("public"), //
				this.S("import"), this.S("grammar"), this.S("example"), //
				this.S("type") //
		), this.Not("@W"));
	}

	public Expression pID() {
		return this.Expr(this.Not("@KEYWORD"), "@LETTER", this.ZeroMore("@W"));
	}

	public Expression pId() {
		return this.Tree("@ID", "#Name");
	}

	public Expression pName() {
		return this.Tree("@LETTER", this.ZeroMore("@W"), "#Name");
	}

	public Expression pLETTER() {
		return this.Range('A', 'Z', 'a', 'z', '_', '_');
	}

	public Expression pW() {
		return this.Range('0', '9', 'A', 'Z', 'a', 'z', '_', '_');
	}

	public Expression pImport() {
		return this.Tree(this.S("import"), ("@_"), this.Link("$name", "@String"), "#Import");
	}

	public Expression pExample() {
		return this.Tree("'example'", "@S", //
				this.Link("$name", "@NonTerminal"), //
				this.Option("@_", this.S("&"), this.Link("$name2", "@NonTerminal")), //
				this.Option("@_", this.S("~"), this.Link("$hash", "@Hash")), //
				this.ZeroMore(this.Range(' ', ' ', '\t', '\t')), //
				this.Choice("@ExampleText1", "@ExampleText2", "@ExampleText3", "@ExampleText4"), "#Example");
	}

	Expression pHash() {
		return this.Tree(this.OneMore(("@HEX")), ("#String"));
	}

	public Expression pHEX() {
		return this.Range('0', '9', 'A', 'F', 'a', 'f');
	}

	Expression pExampleText1() {
		return this.Expr(this.S("'''"), ("@EOL"), this.Link("$text", this.Tree(this.ZeroMore(this.NotAny("\n'''")))),
				("@EOL"), this.S("'''"));
	}

	Expression pExampleText2() {
		return this.Expr(this.S("```"), ("@EOL"), this.Link("$text", this.Tree(this.ZeroMore(this.NotAny("\n```")))),
				("@EOL"), this.S("```"));
	}

	Expression pExampleText3() {
		return this.Expr(this.S("\"\"\""), ("@EOL"),
				this.Link("$text", this.Tree(this.ZeroMore(this.NotAny("\n\"\"\"")))), ("@EOL"), this.S("\"\"\""));
	}

	Expression pExampleText4() {
		return this.Expr(this.Link("$text", this.Tree(this.ZeroMore(this.NotAny(("@EOL"))))), ("@EOL"));
	}

	/* Production */

	public Expression pProduction() {
		return this.Tree(//
				this.Option("'public'", this.Not("@W"), this.Link("$public", this.Tree()), "@_"), //
				this.Link("$name", this.Choice(("@Id"), ("@String"))),
				"@_", /* ("@SKIP"), */ //
				this.S("="), "@_", //
				this.Option(this.S("|"), "@_"), //
				this.Option(this.S("/"), "@_"), //
				this.Link("$expr", "@Expression"), //
				("#Production") //
		);
	}

	public Expression pExpression() {
		return this.Expr("@UChoice");
	}

	public Expression pUChoice() {
		return this.Expr("@Choice",
				this.ZeroMoreFold(null, "@_", this.S("|"), "@_", this.Link(null, "@Choice"), "#UChoice"));
	}

	public Expression pChoice() {
		return this.Expr("@Sequence",
				this.ZeroMoreFold(null, "@_", this.S("/"), "@_", this.Link(null, "@Sequence"), "#Choice"));
	}

	public Expression pSequence() {
		return this.Expr(("@Predicate"), this.ZeroMoreFold(null, "@_",
				/* Not("@NonTerminal", "@_", Choice('=', ':')), */ this.Link(null, "@Predicate"), "#Sequence"));
	}

	public Expression pPredicate() {
		Expression And = this.Expr(this.S("&"), ("#And"));
		Expression Not = this.Expr(this.S("!"), ("#Not"));
		Expression Match = this.Expr(this.S("~"), ("#Detree"));
		return this.Choice(//
				this.Tree(this.Choice(And, Not, Match), this.Link("$expr", "@Suffix")), //
				"@Suffix"//
		);
	}

	public Expression pSuffix() {
		Expression _Zero = this.Expr(this.S("*"), this.Option(this.Link("$max", "@Integer")), ("#Repetition"));
		Expression _One = this.Expr(this.S("+"), this.Option(this.Link("$max", "@Integer")),
				this.Link("$min", this.Tree("`1`")), "#Repetition");
		Expression _Option = this.Expr(this.S("?"), ("#Option"));
		return this.Expr("@Term", this.OptionalFold("expr", this.Choice(_Zero, _One, _Option)));
	}

	public Expression pInteger() {
		return this.Tree("@INT", "#Integer");
	}

	public Expression pDIGIT() {
		return this.Range('0', '9');
	}

	public Expression pINT() {
		return this.Expr("@DIGIT", this.ZeroMore("@DIGIT"));
	}

	public Expression pTerm() {
		Expression x01 = this.Range('0', '1', 'x', 'x', 'X', 'X');
		return this.Choice(//
				"@Character", // '....'
				"@Charset", // [....]
				this.Expr("@String", this.Not("@RULE")), // "....."
				this.Tree(this.S("."), ("#AnyChar")), //
				this.Tree(x01, x01, x01, x01, x01, this.OneMore(x01), "#ByteClass"), //
				this.Tree(this.S("0x"), ("@HEX"), ("@HEX"), "#ByteChar"), //
				this.Tree(this.S("U+"), ("@HEX"), ("@HEX"), ("@HEX"), ("@HEX"), "#ByteChar"), //
				("@Constructor"), //
				("@Link"), //
				("@Replace"), //
				("@Tagging"), //
				this.Expr(this.S("("), "@_", "@Expression", "@_", this.S(")")), //
				("@Func"), //
				this.Expr(("@NonTerminal"), this.Not("@RULE"))//
		);
	}

	Expression pRULE() {
		return this.Expr("@_", this.Choice('=', ':'));
	}

	public Expression pCharacter() {
		Expression StringContent = this
				.ZeroMore(this.Choice(this.S("\\'"), this.S("\\\\"), this.Expr(this.Not(this.S("'")), this.AnyChar())));
		return this.Expr(this.S("'"), this.Tree(StringContent, ("#Character")), this.S("'"));
	}

	public Expression pString() {
		Expression StringContent = this.ZeroMore(
				this.Choice(this.S("\\\""), this.S("\\\\"), this.Expr(this.Not(this.S("\"")), this.AnyChar())));
		return this.Expr(this.S("\""), this.Tree(StringContent, ("#String")), this.S("\""));
	}

	public Expression pCharset() {
		Expression _CharChunk = this.Expr(//
				this.Tree(("@CHAR"), ("#Character")), //
				this.OptionalFold("right", this.S("-"), this.Link("$left", this.Tree(("@CHAR"), ("#Character"))),
						("#Pair"))//
		);
		return this.Expr(//
				this.S("["), //
				this.Tree(this.ZeroMore(this.Link(null, _CharChunk)), ("#Class")), //
				this.S("]")//
		);
	}

	public Expression pCHAR() {
		return this.Choice(//
				this.Expr(this.S("\\u"), ("@HEX"), ("@HEX"), ("@HEX"), ("@HEX")), //
				this.Expr(this.S("\\x"), ("@HEX"), ("@HEX")), this.S("\\n"), this.S("\\t"), this.S("\\\\"),
				this.S("\\r"), this.S("\\v"), this.S("\\f"), this.S("\\-"), this.S("\\]"), //
				this.Expr(this.Not(this.S("]")), this.AnyChar())//
		);
	}

	public Expression pConstructor() {
		return this.Tree(//
				this.S("{"), //
				this.Choice(//
						this.Expr(this.S("$"), this.Option(this.Link("$name", "@Name")), ("@S"), ("#FoldTree")), //
						this.Expr(this.S("@"), ("@S"), ("#FoldTree")), //
						("#Tree")), //
				"@_", //
				this.Option(this.Link("$expr", "@Expression"), "@_"), //
				this.S("}")//
		);
	}

	public Expression pNonTerminal() {
		return this.Tree(("@ID"), this.Option(this.t('.'), ("@ID")), ("#NonTerminal"));
	}

	/**
	 * #ABC #$
	 */

	public Expression pTagName() {
		Expression W = this.Range('A', 'Z', 'a', 'z', '0', '9', '_', '_', '$', '$');
		return this.Tree(W, this.ZeroMore(W), ("#Tagging"));
	}

	public Expression pTagging() {
		return this.Expr(this.Choice(this.t('#'), this.t(':')), ("@TagName"));
	}

	public Expression pReplace() {
		Expression ValueContent = this
				.ZeroMore(this.Choice(this.S("\\`"), this.S("\\\\"), this.Expr(this.Not(this.S("`")), this.AnyChar())));
		return this.Expr(this.S("`"), this.Tree(ValueContent, ("#Replace")), this.S("`"));
	}

	public Expression pLink() {
		return this.Expr(this.t('$'), this.Tree(this.Option(this.Link("$name", "@Name")), "@LinkChoice"));
	}

	Expression pLinkChoice() {
		return this.Choice("@LinkInner", "@LinkTree");
	}

	Expression pLinkInner() {
		return this.Expr(this.S("("), "@_", this.Link("$expr", "@Expression"), "@_", this.S(")"), ("#Link"));
	}

	Expression pLinkTree() {
		return this.Expr(this.S("{"), "@_", this.Link("$expr", "@Expression"), "@_", this.S("}"), ("#LinkTree"));
	}

	public Expression pFunc() {
		return this.Expr(this.S("<"), //
				this.Tree(this.Choice("@IfFunc", "@OnFunc", //
						"@SymbolFunc", "@ExistsFunc", "@MatchFunc", "@IsaFunc", "@IsFunc", //
						"@BlockFunc", "@LocalFunc", //
						"@ScanFunc", "@RepeatFunc", //
						"@UndefinedFunc"//
				)), "@_", this.S(">"));
	}

	Expression pIfFunc() {
		return this.Expr(this.S("if"), "@_", this.Link("$name", "@FlagName"), ("#If"));
	}

	Expression pOnFunc() {
		return this.Expr(this.Choice(this.S("on"), this.S("with")), "@_", this.Link("$name", "@FlagName"), "@_",
				this.Link("$expr", "@Expression"), ("#On"));
	}

	Expression pFlagName() {
		return this.Tree(this.Option("!"), ("@LETTER"), this.ZeroMore(("@W")), ("#Name"));
	}

	// Expression pDefFunc() { // Deprecated
	// return Expr(S("def"), "@_", Link("$name", "@Name"), "@_", Link("$expr",
	// "@Expression"), ("#Def"));
	// }

	Expression pSymbolFunc() {
		return this.Expr(this.S("symbol"), "@_", this.Link("$name", "@Id"), ("#Symbol"));
	}

	Expression pExistsFunc() {
		return this.Expr(this.S("exists"), "@_", this.Link("$name", "@Id"),
				this.Option("@_", this.Link("$symbol", "@Character")), ("#Exists"));
	}

	Expression pMatchFunc() {
		return this.Expr(this.S("match"), "@_", this.Link("$name", "@Id"), ("#Match"));
	}

	Expression pIsFunc() {
		return this.Expr(this.S("is"), "@_", this.Link("$name", "@Id"), ("#Is"));
	}

	Expression pIsaFunc() {
		return this.Expr(this.S("isa"), "@_", this.Link("$name", "@Id"), ("#Isa"));
	}

	Expression pBlockFunc() {
		return this.Expr(this.S("block"), "@_", this.Link("$expr", "@Expression"), ("#Block"));
	}

	Expression pLocalFunc() {
		return this.Expr(this.S("local"), "@_", this.Link("$name", "@Id"), "@_", this.Link("$expr", "@Expression"),
				("#Local"));
	}

	Expression pScanFunc() {
		return this.Expr(this.S("scan"), "@_", this.Option(this.Link("$mask", "@Mask"), "@_"),
				this.Link("$expr", "@Expression"), ("#Scan"));
	}

	Expression pMask() {
		return this.Tree(this.OneMore(this.Range('0', '1')), ("#Name"));
	}

	Expression pRepeatFunc() {
		return this.Expr(this.S("repeat"), "@_", this.Link("$expr", "@Expression"), ("#Repeat"));
	}

	Expression pUndefinedFunc() {
		return this.Expr(this.OneMore(this.Not(">"), this.AnyChar()), ("#Undefined"));
	}

	// public Expression pCase() {
	// return Tree("@_", Link("$case", "@Expression"), "@_", S(":"), "@_",
	// Link("$expr", "@Expression"), "@_", Option("|"), ("#Case"));
	// }

}
