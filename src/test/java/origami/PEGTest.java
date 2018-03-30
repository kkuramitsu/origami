
package origami;

import origami.nez2.PEG;

public class PEGTest {

	public void testLoader() throws Throwable {
		PEG peg = PEG.nez();
		peg.testMatch("COMMENT", "/*hoge*/hoge", "[# '/*hoge*/']");
		peg.testMatch("COMMENT", "//hoge\nhoge", "[# '//hoge']");

		peg.testMatch("Production", "A = a", "?");
		peg.testMatch("NonTerminal", "a", "[#Name 'a']");
		peg.testMatch("Term", "a", "[#Name 'a']");
		peg.testMatch("Expression", "''", "[#Char '']");
		peg.testMatch("Expression", "'a'", "[#Char 'a']");
		peg.testMatch("Expression", "\"a\"", "[#Name '\"a\"']");
		peg.testMatch("Expression", "[a]", "[#Class 'a']");
		peg.testMatch("Expression", "f(a)", "[#Func $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "f(a,b)", "[#Func $=[#Name 'b'] $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "<f a>", "[#Func $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "<f a b>", "[#Func $=[#Name 'b'] $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "&a", "[#And $=[#Name 'a']]");
		peg.testMatch("Expression", "!a", "[#Not $=[#Name 'a']]");
		peg.testMatch("Expression", "a?", "[#Option $=[#Name 'a']]");
		peg.testMatch("Expression", "a*", "[#Many $=[#Name 'a']]");
		peg.testMatch("Expression", "a+", "[#OneMore $=[#Name 'a']]");
		peg.testMatch("Expression", "{a}", "[#Tree $=[#Name 'a']]");
		peg.testMatch("Expression", "{$ a}", "[#Fold $=[#Name 'a']]");
		peg.testMatch("Expression", "$a", "[#Let $=[#Name 'a']]");
		peg.testMatch("Expression", "$(a)", "[#Let $=[#Name 'a']]");
		peg.testMatch("Expression", "$name(a)", "[#Let $=[#Name 'a'] $=[#Name 'name']]");
		peg.testMatch("Expression", "$(name=)a", "[#Let $=[#Name 'a'] $=[#Name 'name']]");

		peg.testMatch("Expression", "a a", "[#Seq $=[#Name 'a'] $=[#Name 'a']]");
		peg.testMatch("Expression", "a b c", "[#Seq $=[#Seq $=[#Name 'c'] $=[#Name 'b']] $=[#Name 'a']]");
		peg.testMatch("Expression", "a/b / c", "[#Or $=[#Or $=[#Name 'c'] $=[#Name 'b']] $=[#Name 'a']]");
		peg.testMatch("Statement", "A=a", "[#Production $=[#Name 'a'] $=[#Name 'A']]");
		peg.testMatch("Statement", "public A=a", "[#Production $=[#Name 'a'] $=[#Name 'A'] $=[# 'public']]");
		peg.testMatch("Statement", "A x = a", "[#Macro $=[#Name 'a'] $=[# $=[#Name 'x']] $=[#Name 'A']]");
		peg.testMatch("Statement", "section ns", "[#Section $=[#Name 'ns']]");
		peg.testMatch("Statement", "example A,B abc \n", "[#Example $=[# 'abc '] $=[# $=[#Name 'B'] $=[#Name 'A']]]");
		peg.testMatch("Statement", "import A,B from 'hogehoge.text'",
				"[#Import $=[#Char 'hogehoge.text'] $=[# $=[#Name 'B'] $=[#Name 'A']]]");
		peg.testMatch("Statement", "A = a\n  b", "[#Production $=[#Seq $=[#Name 'b'] $=[#Name 'a']] $=[#Name 'A']]");
		peg.testMatch("Start", "A = a; B = b;;",
				"[#Source $=[#Production $=[#Name 'b'] $=[#Name 'B']] $=[#Production $=[#Name 'a'] $=[#Name 'A']]]");
		peg.testMatch("Start", "A = a\nB = b",
				"[#Source $=[#Production $=[#Name 'b'] $=[#Name 'B']] $=[#Production $=[#Name 'a'] $=[#Name 'A']]]");
		peg.testMatch("Start", "A = a //hoge\nB = b",
				"[#Source $=[#Production $=[#Name 'b'] $=[#Name 'B']] $=[#Production $=[#Name 'a'] $=[#Name 'A']]]");
	}

	public void testMath() throws Throwable {
		PEG peg = new PEG();
		peg.load("/blue/origami/grammar/math.opeg");
		peg.testMatch("Expression", "1", "[#IntExpr '1']", //
				"1+2", "[#AddExpr $right=[#IntExpr '2'] $left=[#IntExpr '1']]", //
				"1+2*3", "[#AddExpr $right=[#MulExpr $right=[#IntExpr '3'] $left=[#IntExpr '2']] $left=[#IntExpr '1']]", //
				"1*2+3",
				"[#AddExpr $right=[#IntExpr '3'] $left=[#MulExpr $right=[#IntExpr '2'] $left=[#IntExpr '1']]]");
	}

	// public void test_scan() throws Throwable {
	// this.parseExample("scan");
	// }

}
