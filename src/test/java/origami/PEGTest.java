
package origami;

import origami.nez2.Hack;
import origami.nez2.PEG;
import origami.nez2.TPEG;

public class PEGTest {

	public void testNez() throws Throwable {
		PEG nez = PEG.nez();
		nez.testMatch("COMMENT", "/*hoge*/hoge", "[# '/*hoge*/']");
		nez.testMatch("COMMENT", "//hoge\nhoge", "[# '//hoge']");
		nez.testMatch("Doc", "'''\nfunction func(){}\n'''\n", "[# 'function func(){}\n']");

		// peg.testMatch("Production", "A = a", "?");
		nez.testMatch("NonTerminal", "a", "[#Name 'a']");
		nez.testMatch("Term", "a", "[#Name 'a']");
		nez.testMatch("Expression", "''", "[#Char '']");
		nez.testMatch("Expression", "'a'", "[#Char 'a']");
		nez.testMatch("Expression", "\"a\"", "[#Name '\"a\"']");
		nez.testMatch("Expression", "[a]", "[#Class 'a']");
		nez.testMatch("Expression", "f(a)", "[#Func $=[#Name 'a'] $=[#Name 'f']]");
		nez.testMatch("Expression", "f(a,b)", "[#Func $=[#Name 'b'] $=[#Name 'a'] $=[#Name 'f']]");
		nez.testMatch("Expression", "<f a>", "[#Func $=[#Name 'a'] $=[#Name 'f']]");
		nez.testMatch("Expression", "<f a b>", "[#Func $=[#Name 'b'] $=[#Name 'a'] $=[#Name 'f']]");
		nez.testMatch("Expression", "&a", "[#And $=[#Name 'a']]");
		nez.testMatch("Expression", "!a", "[#Not $=[#Name 'a']]");
		nez.testMatch("Expression", "a?", "[#Option $=[#Name 'a']]");
		nez.testMatch("Expression", "a*", "[#Many $=[#Name 'a']]");
		nez.testMatch("Expression", "a+", "[#OneMore $=[#Name 'a']]");
		nez.testMatch("Expression", "{}", "[#Tree $=[#Empty '']]");
		nez.testMatch("Expression", "{ a }", "[#Tree $=[#Name 'a']]");
		nez.testMatch("Expression", "{ }", "[#Tree $=[#Empty '']]");
		nez.testMatch("Expression", "()", "[#Empty '']");
		nez.testMatch("Expression", "&'a'", "[#And $=[#Char 'a']]");

		nez.testMatch("Expression", "{a}", "[#Tree $=[#Name 'a']]");
		nez.testMatch("Expression", "{$ a}", "[#Fold $=[#Name 'a']]");
		nez.testMatch("Expression", "$a", "[#Let $=[#Name 'a']]");
		nez.testMatch("Expression", "$(a)", "[#Let $=[#Name 'a']]");
		nez.testMatch("Expression", "$name(a)", "[#Let $=[#Name 'a'] $=[#Name 'name']]");
		nez.testMatch("Expression", "$(name=)a", "[#Let $=[#Name 'a'] $=[#Name 'name']]");

		nez.testMatch("Func", "<block INDENTBLOCK>");
		nez.testMatch("Expression", "<block INDENT_BLOCK>");

		nez.testMatch("Expression", "a a", "[#Seq $=[#Name 'a'] $=[#Name 'a']]");
		nez.testMatch("Expression", "a b c", "[#Seq $=[#Seq $=[#Name 'c'] $=[#Name 'b']] $=[#Name 'a']]");
		nez.testMatch("Expression", "a/b / c", "[#Or $=[#Or $=[#Name 'c'] $=[#Name 'b']] $=[#Name 'a']]");
		nez.testMatch("Statement", "A=a", "[#Production $=[#Name 'a'] $=[#Name 'A']]");
		nez.testMatch("Statement", "public A=a", "[#Production $=[#Name 'a'] $=[#Name 'A'] $=[# 'public']]");
		nez.testMatch("Statement", "A x = a", "[#Macro $=[#Name 'a'] $=[# $=[#Name 'x']] $=[#Name 'A']]");
		nez.testMatch("Statement", "section ns", "[#Section $=[#Name 'ns']]");
		nez.testMatch("Statement", "example A,B abc \n", "[#Example $=[# 'abc '] $=[# $=[#Name 'B'] $=[#Name 'A']]]");
		nez.testMatch("Statement", "import A,B from 'hogehoge.text'",
				"[#Import $=[#Char 'hogehoge.text'] $=[# $=[#Name 'B'] $=[#Name 'A']]]");
		nez.testMatch("Statement", "A = a\n  b", "[#Production $=[#Seq $=[#Name 'b'] $=[#Name 'a']] $=[#Name 'A']]");
		nez.testMatch("Start", "A = a; B = b;;",
				"[#Source $=[#Production $=[#Name 'b'] $=[#Name 'B']] $=[#Production $=[#Name 'a'] $=[#Name 'A']]]");
		nez.testMatch("Start", "A = a\nB = b",
				"[#Source $=[#Production $=[#Name 'b'] $=[#Name 'B']] $=[#Production $=[#Name 'a'] $=[#Name 'A']]]");
		nez.testMatch("Start", "A = a //hoge\nB = b",
				"[#Source $=[#Production $=[#Name 'b'] $=[#Name 'B']] $=[#Production $=[#Name 'a'] $=[#Name 'A']]]");
	}

	public void testMath() throws Throwable {
		PEG peg = new PEG();
		peg.load("/origami/grammar/math.opeg");
		peg.testMatch("Expression", "1", "[#IntExpr '1']");
		peg.testMatch("Expression", "1+2", "[#AddExpr $right=[#IntExpr '2'] $left=[#IntExpr '1']]"); //
		peg.testMatch("Expression", "1+2*3",
				"[#AddExpr $right=[#MulExpr $right=[#IntExpr '3'] $left=[#IntExpr '2']] $left=[#IntExpr '1']]"); //
		peg.testMatch("Expression", "1*2+3",
				"[#AddExpr $right=[#IntExpr '3'] $left=[#MulExpr $right=[#IntExpr '2'] $left=[#IntExpr '1']]]");
	}

	public void testExpression() throws Throwable {
		/* Empty */
		Hack.expr("''").testMatch("A", "", "[# '']", "a", "[# '']");
		/* Char */
		Hack.expr("'a'").testMatch("A", "aa", "[# 'a']", "b", "[#err* '']");
		Hack.expr("[']").testMatch("A", "''", "[# '\'']");
		Hack.expr("'\\\\]'").testMatch("A", "\\]a", "[# '\\]']");
		/* Class */
		Hack.expr("'\\\\]'").testMatch("A", "\\]a", "[# '\\]']");
		// And
		Hack.expr("&'a'").testMatch("A", "ab", "[# '']");
		Hack.expr("&'a'").testMatch("A", "bb", "[#err* '']");
		// /* Or */
		Hack.expr("a/aa").testMatch("A", "aa", "[# 'a']", "a", "[# 'a']");
		Hack.expr("ab/aa").testMatch("A", "aa", "[# 'aa']", "ab", "[# 'ab']");
		// /* Option */
		Hack.expr("a a?").testMatch("A", "aa", "[# 'aa']", "ab", "[# 'a']");
		Hack.expr("ab ab?").testMatch("A", "abab", "[# 'abab']", "ab", "[# 'ab']");
		/* Many */
		Hack.expr("a*").testMatch("A", "aa", "[# 'aa']", "ab", "[# 'a']", "b", "[# '']");
		Hack.expr("ab*").testMatch("A", "abab", "[# 'abab']", "aba", "[# 'ab']");

		Hack.expr("{a #Hoge}").testMatch("A", "aa", "[#Hoge 'a']");
		Hack.expr("[あ-を]").testMatch("A", "ああ", "[# 'あ']", "を", "[# 'を']");
		//
		Hack.expr(
				"[\\x00-\\x7F] / [\\xC2-\\xDF] [\\x80-\\xBF] / [\\xE0-\\xEF] [\\x80-\\xBF] [\\x80-\\xBF] / [\\xF0-\\xF7] [\\x80-\\xBF] [\\x80-\\xBF] [\\x80-\\xBF]")
				.testMatch("A", "aa", "[# 'a']", "ああ", "[# 'あ']");

		Hack.expr("'\\\\]'").testMatch("A", "\\]a", "[# '\\]']");
		Hack.expr("'\\\\]' / ![\\]] .").testMatch("A", "\\]a", "[# '\\]']");
		Hack.expr("('\\\\]' / ![\\]] .)*").testMatch("A", "a]", "[# 'a']", "a\\]]", "[# 'a\\]']");
		Hack.expr("('\\\\\\'' / !['] .)*").testMatch("A", "a'b", "[# 'a']", "a\\''b", "[# 'a\\'']");
		/* '...' */
		Hack.expr("'\\'' ('\\\\\\'' / !['] .)* '\\''").testMatch("A", "'a'b", "[# ''a'']", "'a\\''b", "[# ''a\\''']");
		/* "..." */
		Hack.expr("'\"' ('\\\\\"' / ![\"] .)* '\"'").testMatch("A", "\"a\"b", "[# '\"a\"']", "\"a\\\"\"b",
				"[# '\"a\\\"\"']");
		/* [...] */
		Hack.expr("'[' ('\\\\]' / ![\\]] .)* ']'").testMatch("A", "[a]b", "[# '[a]']", "[a\\]]b", "[# '[a\\]]']");

		Hack.expr("'\\\\' .").testMatch("A", "\\a", "[# '\\a']");
		Hack.expr("'\\\\' 'a'").testMatch("A", "\\a", "[# '\\a']");
		Hack.expr("'\\\\a'").testMatch("A", "\\a", "[# '\\a']");
		Hack.expr("'a' .").testMatch("A", "ab", "[# 'ab']");
		Hack.expr(".").testMatch("A", "a", "[# 'a']");
		Hack.expr("!.").testMatch("A", "", "[# '']");

		Hack.expr("'[' (('\\\\' .) / (![\\]\\n] .))* ']'").testMatch("A", "[a]", "[# '[a]']", "['\\\"\\\\bfnrt]",
				"[# '['\\\"\\\\bfnrt]']", "[a\\]]b", "[# '[a\\]]']");
		Hack.expr("'[' { ('\\\\]'  / ![\\]\n] .)* #Class } ']'").testMatch("A", "['\\\"\\\\bfnrt]",
				"[#Class ''\\\"\\\\bfnrt']");
	}

	public void testIsUnit() throws Throwable {
		Hack.testFunc("isUnit", ".", e -> TPEG.isUnit(e), "true");
		Hack.testFunc("isUnit", "!{.}", e -> TPEG.isUnit(e), "true");
		Hack.testFunc("isUnit", "!T; T = {.}", e -> TPEG.isUnit(e), "true");
		Hack.testFunc("isUnit", "!M; M = ${.}", e -> TPEG.isUnit(e), "true");
		Hack.testFunc("isUnit", "!F; F = {$ .}", e -> TPEG.isUnit(e), "true");
		Hack.testFunc("isTree", "T; T = {.}", e -> TPEG.isTree(e), "true");

	}

	public void testIsTree() throws Throwable {
		Hack.testFunc("isTree", "T?; T = {.}", e -> TPEG.isTree(e), "true");
		Hack.testFunc("isTree", "./T; T = {.}", e -> TPEG.isTree(e), "true");
		Hack.testFunc("isTree", "T/.; T = {.}", e -> TPEG.isTree(e), "true");
	}

	public void testIsMut() throws Throwable {
		Hack.testFunc("isMut", "M; M = ${.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "T*; T = {.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "T+; T = {.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "&T; T = {.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "M/.; M = ${.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "./M; M = ${.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "M*; M = ${.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "M+; M = ${.}", e -> TPEG.isMut(e), "true");
		Hack.testFunc("isMut", "&M; M = ${.}", e -> TPEG.isMut(e), "true");
	}

	public void testIsFold() throws Throwable {
		// isFold
		Hack.testFunc("isFold", "F; F = {$ .}", e -> TPEG.isFold(e), "true");
		Hack.testFunc("isFold", "F/.; F = {$ .}", e -> TPEG.isFold(e), "true");
		Hack.testFunc("isFold", "./F; F = {$ .}", e -> TPEG.isFold(e), "true");
		Hack.testFunc("isFold", "F*; F = {$ .}", e -> TPEG.isFold(e), "true");

	}

	public void testCheckAST() throws Throwable {
		// AST check
		Hack.testFunc("checkAST", "T; T = {.}", e -> TPEG.checkAST(e), "T");
		Hack.testFunc("checkAST", "M; M = ${.}", e -> TPEG.checkAST(e), "M");
		Hack.testFunc("checkAST", "F; F = ${.}", e -> TPEG.checkAST(e), "F");

		Hack.testFunc("checkAST", ".", e -> TPEG.checkAST(e), ".");
		Hack.testFunc("checkAST", "{.}", e -> TPEG.checkAST(e), "{.}");
		Hack.testFunc("checkAST", "${.}", e -> TPEG.checkAST(e), "$({.})");
		Hack.testFunc("checkAST", "{$ .}", e -> TPEG.checkAST(e), "{$ .}");

		Hack.testFunc("checkAST", ". #t", e -> TPEG.checkAST(e), ". #t");
		Hack.testFunc("checkAST", "{.} #t", e -> TPEG.checkAST(e), "{.}");
		Hack.testFunc("checkAST", "${.} #t", e -> TPEG.checkAST(e), "$({.}) #t");
		Hack.testFunc("checkAST", "{$ .} #t", e -> TPEG.checkAST(e), "{$ .}");

	}

	public void testAutoLinking() throws Throwable {
		// auto linking
		Hack.testFunc("checkAST", "{ $.}", e -> TPEG.checkAST(e), "{$({.})}");
		Hack.testFunc("checkAST", "{T}; T = {.}", e -> TPEG.checkAST(e), "{$(T)}");
		Hack.testFunc("checkAST", "{T T}; T = {.}", e -> TPEG.checkAST(e), "{$(T) $(T)}");
		Hack.testFunc("checkAST", "{T?}; T = {.}", e -> TPEG.checkAST(e), "{$(T)?}");
		Hack.testFunc("checkAST", "{T/.}; T = {.}", e -> TPEG.checkAST(e), "{$(T) / .}"); // FIXME

	}

	public void testAutoLeftFolding() throws Throwable {
		Hack.testFunc("checkAST", "{.} {.}", e -> TPEG.checkAST(e), "{.} {$ $({.})}");
		Hack.testFunc("checkAST", "{.} {.}*", e -> TPEG.checkAST(e), "{.} {$ $({.})}*");
		Hack.testFunc("checkAST", "T T; T = {.}", e -> TPEG.checkAST(e), "T {$ $(T)}");
		Hack.testFunc("checkAST", "T T*; T = {.}", e -> TPEG.checkAST(e), "T {$ $(T)}*");

	}

	public void testNotUnit() throws Throwable {
		Hack.testFunc("checkAST", "!{.}", e -> TPEG.checkAST(e), "!.");
		Hack.testFunc("checkAST", "!T; T = {.}", e -> TPEG.checkAST(e), "!untree(T)");
		Hack.testFunc("checkAST", "!M; M = ${.}", e -> TPEG.checkAST(e), "!untree(M)");
		Hack.testFunc("checkAST", "!(T #t); T = {.}", e -> TPEG.checkAST(e), "!untree(T)");

	}

	// public void test_scan() throws Throwable {
	// this.parseExample("scan");
	// }

}
