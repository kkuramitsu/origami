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

package origami.nez.peg;

public class NezGrammar extends ParserCombinator {

	public Expression pStart() {
		return Expr("@_", Choice("@Expression", "@Source"), "@EOT");
	}

	public Expression pSource() {
		return Expr("@_", Tree(ZeroMore(Link(null, "@Stmt")), "#Source"));
	}

	public Expression p_() {
		return ZeroMore(Choice("@S", "@COMMENT"));
	}

	public Expression pS() {
		return Choice(//
				Range(' ', ' ', '\t', '\t', '\r', '\r', '\n', '\n'), //
				S("\u3000")//
		);
	}

	public Expression pCOMMENT() {
		return Choice(//
				Expr(S("/*"), ZeroMore(Not(S("*/")), AnyChar()), S("*/")), //
				Expr(S("//"), ZeroMore(Not(("@EOL")), AnyChar()), ("@EOL")), //
				Expr(S("format"), "@S", S("#"), ZeroMore(NotAny('\n')))//
		);
	}

	public Expression pEOL() {
		return Choice("'\n'", Expr("'\r'", "'\n'"), "@EOT");
	}

	public Expression pEOT() {
		return Not(AnyChar());
	}

	public Expression pStmt() {
		return Expr(Choice("@Grammar", "@Import", "@Example", "@Production"), "@_", Option(S(";"), "@_"));
	}

	public Expression pGrammar() {
		return Tree(S("grammar"), ("@_"), //
				Link("$name", "@Name"), ("@_"), //
				S("{"), ("@_"), //
				Link("$body", "@Source"), //
				S("}"), //
				"#Grammar"//
		);
	}

	public Expression pKEYWORD() {
		return Expr(Choice(//
				S("public"), //
				S("import"), S("grammar"), S("example"), //
				S("type") //
		), Not("@W"));
	}

	public Expression pID() {
		return Expr(Not("@KEYWORD"), "@LETTER", ZeroMore("@W"));
	}

	public Expression pId() {
		return Tree("@ID", "#Name");
	}

	public Expression pName() {
		return Tree("@LETTER", ZeroMore("@W"), "#Name");
	}

	public Expression pLETTER() {
		return Range('A', 'Z', 'a', 'z', '_', '_');
	}

	public Expression pW() {
		return Range('0', '9', 'A', 'Z', 'a', 'z', '_', '_');
	}

	public Expression pImport() {
		return Tree(S("import"), ("@_"), Link("$name", "@String"), "#Import");
	}

	public Expression pExample() {
		return Tree("'example'", "@S", //
				Link("$name", "@NonTerminal"), //
				Option("@_", S("&"), Link("$name2", "@NonTerminal")), //
				Option("@_", S("~"), Link("$hash", "@Hash")), //
				ZeroMore(Range(' ', ' ', '\t', '\t')), //
				Choice("@ExampleText1", "@ExampleText2", "@ExampleText3", "@ExampleText4"), "#Example");
	}

	Expression pHash() {
		return Tree(OneMore(("@HEX")), ("#String"));
	}

	public Expression pHEX() {
		return Range('0', '9', 'A', 'F', 'a', 'f');
	}

	Expression pExampleText1() {
		return Expr(S("'''"), ("@EOL"), Link("$text", Tree(ZeroMore(NotAny("\n'''")))), ("@EOL"), S("'''"));
	}

	Expression pExampleText2() {
		return Expr(S("```"), ("@EOL"), Link("$text", Tree(ZeroMore(NotAny("\n```")))), ("@EOL"), S("```"));
	}

	Expression pExampleText3() {
		return Expr(S("\"\"\""), ("@EOL"), Link("$text", Tree(ZeroMore(NotAny("\n\"\"\"")))), ("@EOL"), S("\"\"\""));
	}

	Expression pExampleText4() {
		return Expr(Link("$text", Tree(ZeroMore(NotAny(("@EOL"))))), ("@EOL"));
	}

	/* Production */

	public Expression pProduction() {
		return Tree(//
				Option("'public'", Not("@W"), Link("$public", Tree()), "@_"), //
				Link("$name", Choice(("@Id"), ("@String"))),
				"@_", /* ("@SKIP"), */ //
				S("="), "@_", //
				Link("$expr", "@Expression"), //
				("#Production") //
		);
	}

	public Expression pExpression() {
		return Expr("@UChoice");
	}

	public Expression pUChoice() {
		return Expr("@Choice", ZeroMoreFold(null, "@_", S("|"), "@_", Link(null, "@Choice"), "#UChoice"));
	}

	public Expression pChoice() {
		return Expr("@Sequence", ZeroMoreFold(null, "@_", S("/"), "@_", Link(null, "@Sequence"), "#Choice"));
	}

	public Expression pSequence() {
		return Expr(("@Predicate"), ZeroMoreFold(null, "@_",
				/* Not("@NonTerminal", "@_", Choice('=', ':')), */ Link(null, "@Predicate"), "#Sequence"));
	}

	public Expression pPredicate() {
		Expression And = Expr(S("&"), ("#And"));
		Expression Not = Expr(S("!"), ("#Not"));
		Expression Match = Expr(S("~"), ("#Detree"));
		return Choice(//
				Tree(Choice(And, Not, Match), Link("$expr", "@Suffix")), //
				"@Suffix"//
		);
	}

	public Expression pSuffix() {
		Expression _Zero = Expr(S("*"), Option(Link("$max", "@Integer")), ("#Repetition"));
		Expression _One = Expr(S("+"), Option(Link("$max", "@Integer")), Link("$min", Tree("`1`")), "#Repetition");
		Expression _Option = Expr(S("?"), ("#Option"));
		return Expr("@Term", OptionalFold("expr", Choice(_Zero, _One, _Option)));
	}

	public Expression pInteger() {
		return Tree("@INT", "#Integer");
	}

	public Expression pDIGIT() {
		return Range('0', '9');
	}

	public Expression pINT() {
		return Expr("@DIGIT", ZeroMore("@DIGIT"));
	}

	public Expression pTerm() {
		Expression x01 = Range('0', '1', 'x', 'x', 'X', 'X');
		return Choice(//
				"@Character", // '....'
				"@Charset", // [....]
				Expr("@String", Not("@RULE")), // "....."
				Tree(S("."), ("#AnyChar")), //
				Tree(x01, x01, x01, x01, x01, OneMore(x01), "#ByteClass"), //
				Tree(S("0x"), ("@HEX"), ("@HEX"), "#ByteChar"), //
				Tree(S("U+"), ("@HEX"), ("@HEX"), ("@HEX"), ("@HEX"), "#ByteChar"), //
				("@Constructor"), //
				("@Link"), //
				("@Replace"), //
				("@Tagging"), //
				Expr(S("("), "@_", "@Expression", "@_", S(")")), //
				("@Func"), //
				Expr(("@NonTerminal"), Not("@RULE"))//
		);
	}

	Expression pRULE() {
		return Expr("@_", Choice('=', ':'));
	}

	public Expression pCharacter() {
		Expression StringContent = ZeroMore(Choice(S("\\'"), S("\\\\"), Expr(Not(S("'")), AnyChar())));
		return Expr(S("'"), Tree(StringContent, ("#Character")), S("'"));
	}

	public Expression pString() {
		Expression StringContent = ZeroMore(Choice(S("\\\""), S("\\\\"), Expr(Not(S("\"")), AnyChar())));
		return Expr(S("\""), Tree(StringContent, ("#String")), S("\""));
	}

	public Expression pCharset() {
		Expression _CharChunk = Expr(//
				Tree(("@CHAR"), ("#Character")), //
				OptionalFold("right", S("-"), Link("$left", Tree(("@CHAR"), ("#Character"))), ("#Pair"))//
		);
		return Expr(//
				S("["), //
				Tree(ZeroMore(Link(null, _CharChunk)), ("#Class")), //
				S("]")//
		);
	}

	public Expression pCHAR() {
		return Choice(//
				Expr(S("\\u"), ("@HEX"), ("@HEX"), ("@HEX"), ("@HEX")), //
				Expr(S("\\x"), ("@HEX"), ("@HEX")), S("\\n"), S("\\t"), S("\\\\"), S("\\r"), S("\\v"), S("\\f"), S("\\-"), S("\\]"), //
				Expr(Not(S("]")), AnyChar())//
		);
	}

	public Expression pConstructor() {
		return Tree(//
				S("{"), //
				Choice(//
						Expr(S("$"), Option(Link("$name", "@Name")), ("@S"), ("#FoldTree")), //
						Expr(S("@"), ("@S"), ("#FoldTree")), //
						("#Tree")), //
				"@_", //
				Option(Link("$expr", "@Expression"), "@_"), //
				S("}")//
		);
	}

	public Expression pNonTerminal() {
		return Tree(("@ID"), Option(t('.'), ("@ID")), ("#NonTerminal"));
	}

	/**
	 * #ABC #$
	 */

	public Expression pTagName() {
		Expression W = Range('A', 'Z', 'a', 'z', '0', '9', '_', '_', '$', '$');
		return Tree(W, ZeroMore(W), ("#Tagging"));
	}

	public Expression pTagging() {
		return Expr(Choice(t('#'), t(':')), ("@TagName"));
	}

	public Expression pReplace() {
		Expression ValueContent = ZeroMore(Choice(S("\\`"), S("\\\\"), Expr(Not(S("`")), AnyChar())));
		return Expr(S("`"), Tree(ValueContent, ("#Replace")), S("`"));
	}

	public Expression pLink() {
		return Expr(t('$'), Tree(Option(Link("$name", "@Name")), "@LinkChoice"));
	}

	Expression pLinkChoice() {
		return Choice("@LinkInner", "@LinkTree");
	}

	Expression pLinkInner() {
		return Expr(S("("), "@_", Link("$expr", "@Expression"), "@_", S(")"), ("#Link"));
	}

	Expression pLinkTree() {
		return Expr(S("{"), "@_", Link("$expr", "@Expression"), "@_", S("}"), ("#LinkTree"));
	}

	public Expression pFunc() {
		return Expr(S("<"), //
				Tree(Choice("@IfFunc", "@OnFunc", //
						"@SymbolFunc", "@ExistsFunc", "@MatchFunc", "@IsaFunc", "@IsFunc", //
						"@BlockFunc", "@LocalFunc", //
						"@ScanFunc", "@RepeatFunc", //
						"@UndefinedFunc"//
				)), "@_", S(">"));
	}

	Expression pIfFunc() {
		return Expr(S("if"), "@_", Link("$name", "@FlagName"), ("#If"));
	}

	Expression pOnFunc() {
		return Expr(Choice(S("on"), S("with")), "@_", Link("$name", "@FlagName"), "@_", Link("$expr", "@Expression"), ("#On"));
	}

	Expression pFlagName() {
		return Tree(Option("!"), ("@LETTER"), ZeroMore(("@W")), ("#Name"));
	}

	// Expression pDefFunc() { // Deprecated
	// return Expr(S("def"), "@_", Link("$name", "@Name"), "@_", Link("$expr",
	// "@Expression"), ("#Def"));
	// }

	Expression pSymbolFunc() {
		return Expr(S("symbol"), "@_", Link("$name", "@Id"), ("#Symbol"));
	}

	Expression pExistsFunc() {
		return Expr(S("exists"), "@_", Link("$name", "@Id"), Option("@_", Link("$symbol", "@Character")), ("#Exists"));
	}

	Expression pMatchFunc() {
		return Expr(S("match"), "@_", Link("$name", "@Id"), ("#Match"));
	}

	Expression pIsFunc() {
		return Expr(S("is"), "@_", Link("$name", "@Id"), ("#Is"));
	}

	Expression pIsaFunc() {
		return Expr(S("isa"), "@_", Link("$name", "@Id"), ("#Isa"));
	}

	Expression pBlockFunc() {
		return Expr(S("block"), "@_", Link("$expr", "@Expression"), ("#Block"));
	}

	Expression pLocalFunc() {
		return Expr(S("local"), "@_", Link("$name", "@Id"), "@_", Link("$expr", "@Expression"), ("#Local"));
	}

	Expression pScanFunc() {
		return Expr(S("scan"), "@_", Option(Link("$mask", "@Mask"), "@_"), Link("$expr", "@Expression"), ("#Scan"));
	}

	Expression pMask() {
		return Tree(OneMore(Range('0', '1')), ("#Name"));
	}

	Expression pRepeatFunc() {
		return Expr(S("repeat"), "@_", Link("$expr", "@Expression"), ("#Repeat"));
	}

	Expression pUndefinedFunc() {
		return Expr(OneMore(Not(">"), AnyChar()), ("#Undefined"));
	}

	// public Expression pCase() {
	// return Tree("@_", Link("$case", "@Expression"), "@_", S(":"), "@_",
	// Link("$expr", "@Expression"), "@_", Option("|"), ("#Case"));
	// }

}
