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

import java.io.IOException;
import java.util.List;

import origami.main.OOption;
import origami.nez.ast.Source;
import origami.nez.ast.SourceLogger;
import origami.nez.ast.Symbol;
import origami.nez.ast.Tree;
import origami.nez.ast.TreeVisitorMap;
import origami.nez.parser.ParserSource;
import origami.nez.parser.Parser;
import origami.trait.OStringUtils;

public class GrammarParser extends TreeVisitorMap<GrammarParser.ExpressionTransducer> {

	public final static Symbol _Source = Symbol.unique("Source");
	public final static Symbol _Import = Symbol.unique("Import");
	public final static Symbol _Grammar = Symbol.unique("Grammar");
	public final static Symbol _Production = Symbol.unique("Production");
	public final static Symbol _body = Symbol.unique("body");
	public final static Symbol _public = Symbol.unique("public");

	public final static Symbol _name = Symbol.unique("name");
	public final static Symbol _expr = Symbol.unique("expr");
	public final static Symbol _symbol = Symbol.unique("symbol");
	public final static Symbol _min = Symbol.unique("min");
	public final static Symbol _mask = Symbol.unique("mask"); // <scanf >

	public final static Symbol _String = Symbol.unique("String");

	public static interface ExpressionTransducer {
		public Expression accept(Tree<?> node, Expression next) throws IOException;
	}

	final SourceLogger logger;
	final Grammar grammar;

	public GrammarParser(Grammar grammar) {
		this(null, grammar);
	}

	public GrammarParser(SourceLogger logger, Grammar grammar) {
		this.logger = logger == null ? new SourceLogger.SimpleSourceLogger() : logger;
		this.grammar = grammar;
		init(GrammarParser.class, new SyntaxRule());
	}
	
	/**
	 * NezParser
	 */

	public static final Parser NezParser;

	static {
		Grammar grammar = new Grammar("nez");
		OOption options = new OOption();
		//options.setVerboseMode(false);
		new NezGrammar().load(grammar, "Start", options);
		// grammar.dump();
		NezParser = grammar.newParser(options);
	}

	public void importSource(Source s) throws IOException {
		Tree<?> t = NezParser.parse(s);
		this.find(key(t)).accept(t, null);
	}
	
	public class SyntaxRule implements ExpressionTransducer {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			//undefined(node);
			return null;
		}
	}

	private final String key(Tree<?> node) {
		return node.getTag().getSymbol();
	}

	/* Expression */
	
	public Expression newExpression(Tree<?> node) throws IOException {
		return this.find(key(node)).accept(node, null);
	}
	
	public class _NonTerminal extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String name = node.toText();

			return new Expression.PNonTerminal(grammar, name, node);
		}
	}

	public class _String extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression es) throws IOException {
			String name = Production.terminalName(node.toText());
			return new Expression.PNonTerminal(grammar, name, node);
		}
	}

	public class _Character extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return Expression.newString(OStringUtils.unquoteString(node.toText()), node);
		}
	}

	public class _Class extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			List<Expression> l = Expression.newList(2);
			if (node.size() > 0) {
				for (int i = 0; i < node.size(); i++) {
					Tree<?> o = node.get(i);
					if (o.size() == 2) { // range
						l.add(newCharSet(node, o.getText(0, ""), o.getText(1, "")));
					} else { // single
						l.add(newCharSet(node, o.toText(), o.toText()));
					}
				}
			}
			return Expression.newChoice(l, node);
		}

		private Expression newCharSet(Object ref, String t, String t2) {
			int c = OStringUtils.parseAscii(t);
			int c2 = OStringUtils.parseAscii(t2);
			if (c != -1 && c2 != -1) {
				return Expression.newRange(c, c2, ref);
			}
			c = OStringUtils.parseUnicode(t);
			c2 = OStringUtils.parseUnicode(t2);
			if (c < 128 && c2 < 128) {
				return Expression.newRange(c, c2, ref);
			} else {
				return newUnicodeRange(ref, c, c2);
			}
		}

		private Expression newUnicodeRange(Object ref, int c, int c2) {
			byte[] b = OStringUtils.utf8(String.valueOf((char) c));
			byte[] b2 = OStringUtils.utf8(String.valueOf((char) c2));
			if (equalsBase(b, b2)) {
				return newUnicodeRange(ref, b, b2);
			}
			List<Expression> l = Expression.newList(b.length);
			b2 = b;
			for (int pc = c + 1; pc <= c2; pc++) {
				byte[] b3 = OStringUtils.utf8(String.valueOf((char) pc));
				if (equalsBase(b, b3)) {
					b2 = b3;
					continue;
				}
				l.add(newUnicodeRange(ref, b, b2));
				b = b3;
				b2 = b3;
			}
			b2 = OStringUtils.utf8(String.valueOf((char) c2));
			l.add(newUnicodeRange(ref, b, b2));
			return Expression.newChoice(l, ref);
		}

		private boolean equalsBase(byte[] b, byte[] b2) {
			if (b.length == b2.length) {
				switch (b.length) {
				case 3:
					return b[0] == b2[0] && b[1] == b2[1];
				case 4:
					return b[0] == b2[0] && b[1] == b2[1] && b[2] == b2[2];
				}
				return b[0] == b2[0];
			}
			return false;
		}

		private Expression newUnicodeRange(Object ref, byte[] b, byte[] b2) {
			if (b[b.length - 1] == b2[b.length - 1]) {
				return Expression.newString(b, ref);
			} else {
				List<Expression> l = Expression.newList(b.length);
				for (int i = 0; i < b.length - 1; i++) {
					l.add(new Expression.PByte(b[i], ref));
				}
				l.add(Expression.newRange(b[b.length - 1] & 0xff, b2[b2.length - 1] & 0xff, ref));
				return Expression.newSequence(l, ref);
			}
		}
	}

	public class _ByteChar extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String t = node.toText();
			if (t.startsWith("U+")) {
				int c = parseHexicalNumber(t.charAt(2));
				c = (c * 16) + parseHexicalNumber(t.charAt(3));
				c = (c * 16) + parseHexicalNumber(t.charAt(4));
				c = (c * 16) + parseHexicalNumber(t.charAt(5));
				if (c < 128) {
					return new Expression.PByte(c, node);
				}
				String t2 = String.valueOf((char) c);
				return Expression.newString(t2, node);
			}
			int c = parseHexicalNumber(t.charAt(t.length() - 2)) * 16 + parseHexicalNumber(t.charAt(t.length() - 1));
			return new Expression.PByte(c, node);
		}

		private int parseHexicalNumber(int c) {
			if ('0' <= c && c <= '9') {
				return c - '0';
			}
			if ('a' <= c && c <= 'f') {
				return c - 'a' + 10;
			}
			if ('A' <= c && c <= 'F') {
				return c - 'A' + 10;
			}
			return 0;
		}

	}

	public class _ByteClass extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String t = node.toText();
			return parseByteClass(t, node);
		}

		private Expression.PByteSet parseByteClass(String octet, Object ref) {
			Expression.PByteSet b = new Expression.PByteSet(ref);
			b.set(0, 255, true);
			while (octet.length() < 8) {
				octet = "0" + octet;
			}
			for (int i = 0; i < 8; i++) {
				int position = 0x80 >> i;
				switch (octet.charAt(i)) {
				case '0':
					for (int j = 0; j < 256; j++) {
						if ((j & position) == 0) {
							continue;
						}
						b.set(j, false);
					}
					break;
				case '1':
					for (int j = 0; j < 256; j++) {
						if ((j & position) != 0) {
							continue;
						}
						b.set(j, false);
					}
					break;
				case 'x':
				default:
					break;
				}
			}
			return b;
		}

	}

	public class _AnyChar extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PAny(node);
		}
	}

	public class _Choice extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			List<Expression> l = Expression.newList(node.size());
			for (int i = 0; i < node.size(); i++) {
				Expression.addChoice(l, newExpression(node.get(i)));
			}
			return Expression.newChoice(l, node);
		}
	}

	public class _Sequence extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			List<Expression> l = Expression.newList(node.size());
			for (int i = 0; i < node.size(); i++) {
				Expression.addSequence(l, newExpression(node.get(i)));
			}
			return Expression.newSequence(l, node);
		}
	}

	public class _Not extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PNot(newExpression(node.get(_expr)), node);
		}
	}

	public class _And extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PAnd(newExpression(node.get(_expr)), node);
		}
	}

	public class _Option extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.POption(newExpression(node.get(_expr)), node);
		}
	}

	public class _Repetition extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			int min = 0;
			if (node.get(_min, null) != null) {
				min = 1;
			}
			return new Expression.PRepetition(newExpression(node.get(_expr)), min, node);
		}
	}

	// PEG4d TransCapturing

	public class _Tree extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expression.defaultEmpty : newExpression(exprNode);
			return Expression.newTree(p, node);
		}
	}

	private Symbol parseLabel(Tree<?> node) {
		Symbol label = null;
		Tree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = Symbol.unique(labelNode.toText());
		}
		return label;
	}

	public class _FoldTree extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expression.defaultEmpty : newExpression(exprNode);
			return Expression.newFoldTree(parseLabel(node), p, node);
		}
	}

	public class _Link extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PLinkTree(parseLabel(node), newExpression(node.get(_expr)), node);
		}
	}

	public class _Tagging extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PTag(Symbol.unique(node.toText()), node);
		}
	}

	public class _Replace extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PReplace(node.toText(), node);
		}
	}

	public class _If extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PIfCondition(node.getText(_name, ""), node);
		}
	}

	public class _On extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.POnCondition(node.getText(_name, ""), newExpression(node.get(_expr)), node);
		}
	}

	public class _Block extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PSymbolScope(NezFunc.block, null, newExpression(node.get(_expr)), node);
		}
	}

	public class _Local extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PSymbolScope(NezFunc.local, Symbol.unique(node.getText(_name, "")), newExpression(node.get(_expr)), node);
		}
	}

	// public class _Def extends TreeVisitor {
	// @Override
	// public Expression accept(Tree<?> node, Expression e) {
	// Grammar g = getGrammar();
	// Tree<?> nameNode = node.get(_name);
	// Expression.NonTerminal pat = new Expression.NonTerminal(g,
	// nameNode.toText(), node);
	// Expression expr = newInstance(node.get(_expr));
	// Production p = g.addProduction(pat.getLocalName(), expr);
	// reportWarning(nameNode, "new production generated: " + p);
	// return Expressions.newSymbol(node, pat);
	// }
	// }

	public class _Symbol extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String param = node.getText(_name, "");
			Expression.PNonTerminal pat = new Expression.PNonTerminal(grammar, param, node);
			return new Expression.PSymbolAction(NezFunc.symbol, param, pat, node);
		}
	}

	public class _Is extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String param = node.getText(_name, "");
			Expression.PNonTerminal pat = new Expression.PNonTerminal(grammar, param, node);
			return new Expression.PSymbolPredicate(NezFunc.is, param, pat, node);
		}
	}

	public class _Isa extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String param = node.getText(_name, "");
			Expression.PNonTerminal pat = new Expression.PNonTerminal(grammar, param, node);
			return new Expression.PSymbolPredicate(NezFunc.isa, param, pat, node);
		}
	}

	public class _Match extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String param = node.getText(_name, "");
			Expression.PNonTerminal pat = new Expression.PNonTerminal(grammar, param, node);
			return new Expression.PSymbolPredicate(NezFunc.match, param, pat, node);
		}
	}

	public class _Exists extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String param = node.getText(_name, "");
			Expression.PNonTerminal pat = new Expression.PNonTerminal(grammar, param, node);
			String symbol = node.getText(_symbol, null);
			if (symbol != null) {
				param = param + "+" + symbol;
			}
			return new Expression.PSymbolPredicate(NezFunc.exists, param, pat, node);
		}
	}

	public class _Scanf extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String mask = node.getText(_mask, null);
			return new Expression.PScan(mask, newExpression(node.get(_expr)), node);
		}
	}

	public class _Repeat extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return new Expression.PRepeat(newExpression(node.get(_expr)), node);
		}
	}

	// public class _Dispatch extends TreeVisitor {
	// @Override
	// public Expression accept(Tree<?> node, Expression e) {
	// Expression[] inners = new Expression[node.size() + 1];
	// byte[] indexMap = new byte[256];
	// inners[0] = Expressions.newFail();
	// int count = 1;
	// for (Tree<?> c : node) {
	// Expression cond = newInstance(c.get(_case));
	// if (cond instanceof Nez.Byte) {
	// indexMap[((Nez.Byte) cond).byteChar] = (byte) count;
	// } else if (cond instanceof Nez.ByteSet) {
	// boolean[] b = ((Nez.ByteSet) cond).byteset;
	// for (int i = 0; i < 256; i++) {
	// if (b[i]) {
	// indexMap[i] = (byte) count;
	// }
	// }
	// } else {
	// factory.verbose("not character: " + cond);
	// }
	// inners[count++] = newInstance(c.get(_expr));
	// }
	// return Expressions.newDispatch(inners, indexMap);
	// }
	// }
	
	/* nez construction */
	
	public class _Production extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			Tree<?> nameNode = node.get(_name);
			//boolean isPublic = node.get(_public, null) != null;
			String name = nameNode.toText();
			if (nameNode.is(_String)) {
				name = Production.terminalName(name);
			}
			Expression rule = grammar.getLocalExpression(name);
			if (rule != null) {
				logger.reportWarning(node.get(_name), NezFmt.YY0_is_duplicated_name, name);
				return rule;
			}
			rule = newExpression(node.get(_expr));
			grammar.addProduction(name, rule);
			return rule;
		}
	}

	public class _Source extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			for (Tree<?> sub : node) {
				newExpression(sub);
			}
			return null;
		}
	}

	public class _Grammar extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String name = node.getText(_name, null);
			Grammar g = new Grammar(name, grammar);
			GrammarParser parser = new GrammarParser(logger, g);
			return parser.newExpression(node.get(_body));
		}
	}

	public class _Import extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			String name = node.getText(_name, null);
			String path = name;
			if (!name.startsWith("/") && !name.startsWith("\\")) {
				path = extractFilePath(node.getSource().getResourceName()) + "/" + name;
			}
			importSource(ParserSource.newFileSource(path, null));
			return null;
		}
	}

	public class _Example extends SyntaxRule {
		@Override
		public Expression accept(Tree<?> node, Expression e) throws IOException {
			return null;
		}
	}

	public final static String extractFilePath(String path) {
		int loc = path.lastIndexOf('/');
		if (loc > 0) {
			return path.substring(0, loc);
		}
		loc = path.lastIndexOf('\\');
		if (loc > 0) {
			return path.substring(0, loc);
		}
		return path;
	}
//
//	public final static String extractFileName(String path) {
//		int loc = path.lastIndexOf('/');
//		if (loc > 0) {
//			return path.substring(loc + 1);
//		}
//		loc = path.lastIndexOf('\\');
//		if (loc > 0) {
//			return path.substring(loc + 1);
//		}
//		return path;
//	}
//
//	public final static String extractFileExtension(String path) {
//		int loc = path.lastIndexOf('.');
//		if (loc > 0) {
//			return path.substring(loc + 1);
//		}
//		return path;
//	}
//
//	public final static String changeFileExtension(String path, String ext) {
//		int loc = path.lastIndexOf('.');
//		if (loc > 0) {
//			return path.substring(0, loc + 1) + ext;
//		}
//		return path + "." + ext;
//	}


}
