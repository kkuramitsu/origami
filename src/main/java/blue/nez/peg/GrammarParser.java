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

import java.io.IOException;
import java.util.List;

import blue.nez.ast.Source;
import blue.nez.ast.SourceLogger;
import blue.nez.ast.SourcePosition;
import blue.nez.ast.Symbol;
import blue.nez.ast.Tree;
import blue.nez.ast.TreeVisitorMap;
import blue.nez.parser.Parser;
import blue.nez.parser.ParserSource;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;

public class GrammarParser extends TreeVisitorMap<GrammarParser.ExpressionTransducer> {

	public final static Symbol _Source = Symbol.unique("Source");
	public final static Symbol _Import = Symbol.unique("Import");
	public final static Symbol _Grammar = Symbol.unique("Grammar");
	public final static Symbol _Production = Symbol.unique("Production");
	public final static Symbol _body = Symbol.unique("body");
	public final static Symbol _public = Symbol.unique("public");

	public final static Symbol _ns = Symbol.unique("ns");
	public final static Symbol _name = Symbol.unique("name");
	public final static Symbol _expr = Symbol.unique("expr");
	public final static Symbol _symbol = Symbol.unique("symbol");
	public final static Symbol _min = Symbol.unique("min");
	public final static Symbol _mask = Symbol.unique("mask"); // <scanf >

	public final static Symbol _String = Symbol.unique("String");

	public static interface ExpressionTransducer {
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException;
	}

	public GrammarParser() {
		this.init(GrammarParser.class, new SyntaxRule());
	}

	/**
	 * OPegParser
	 */

	public static final Grammar OPegGrammar;
	public static final Parser OPegParser;

	static {
		OPegGrammar = new SourceGrammar("opeg");
		OOption options = new OOption();
		// options.setVerboseMode(false);
		new OPegGrammar().load(OPegGrammar, "Start", options);
		// grammar.dump();
		OPegParser = OPegGrammar.newParser(options);
	}

	class Gamma {
		final SourceLogger logger;
		final Grammar grammar;

		Gamma(Grammar grammar, SourceLogger logger) {
			this.grammar = grammar;
			this.logger = logger == null ? new SourceLogger.SimpleSourceLogger() : logger;
		}

		public Expression newExpression(Tree<?> node) throws IOException {
			return GrammarParser.this.find(key(node)).accept(this, node);
		}

		public Gamma newLocalGramma(SourcePosition s, String name) {
			Grammar g = this.grammar.getGrammar(name);
			if (g != null) {
				this.logger.reportWarning(s, NezFmt.YY0_is_duplicated_name, name);
			} else {
				g = this.grammar.newLocalGrammar(name);
			}
			return new Gamma(g, this.logger);
		}

		public void importSource(Source s) throws IOException {
			Tree<?> t = OPegParser.parse(s);
			GrammarParser.this.find(key(t)).accept(this, t);
		}

	}

	// final SourceLogger logger;
	// final Grammar grammar;

	public void importSource(Grammar base, Source s) throws IOException {
		Gamma gamma = new Gamma(base, null);
		gamma.importSource(s);
	}

	private static String key(Tree<?> node) {
		return node.getTag().getSymbol();
	}

	public class SyntaxRule implements ExpressionTransducer {
		@Override
		public Expression accept(Gamma e, Tree<?> node) throws IOException {
			// undefined(node);
			return null;
		}
	}

	/* Expression */

	public class _NonTerminal extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String ns = null;
			String name = node.toText();
			int loc = name.indexOf('.');
			if (loc > 0) {
				ns = name.substring(0, loc);
				name = name.substring(loc + 1);
			}
			return new PNonTerminal(gamma.grammar, ns, name, node);
		}
	}

	public class _String extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String name = Production.terminalName(node.toText());
			return new PNonTerminal(gamma.grammar, name, node);
		}
	}

	public class _Character extends SyntaxRule {
		@Override
		public Expression accept(Gamma e, Tree<?> node) throws IOException {
			return Expression.newString(OStringUtils.unquoteString(node.toText()), node);
		}
	}

	public class _Class extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			List<Expression> l = Expression.newList(2);
			if (node.size() > 0) {
				for (int i = 0; i < node.size(); i++) {
					Tree<?> o = node.get(i);
					if (o.size() == 2) { // range
						l.add(this.newCharSet(node, o.getText(0, ""), o.getText(1, "")));
					} else { // single
						l.add(this.newCharSet(node, o.toText(), o.toText()));
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
				return this.newUnicodeRange(ref, c, c2);
			}
		}

		private Expression newUnicodeRange(Object ref, int c, int c2) {
			byte[] b = OStringUtils.utf8(String.valueOf((char) c));
			byte[] b2 = OStringUtils.utf8(String.valueOf((char) c2));
			if (this.equalsBase(b, b2)) {
				return this.newUnicodeRange(ref, b, b2);
			}
			List<Expression> l = Expression.newList(b.length);
			b2 = b;
			for (int pc = c + 1; pc <= c2; pc++) {
				byte[] b3 = OStringUtils.utf8(String.valueOf((char) pc));
				if (this.equalsBase(b, b3)) {
					b2 = b3;
					continue;
				}
				l.add(this.newUnicodeRange(ref, b, b2));
				b = b3;
				b2 = b3;
			}
			b2 = OStringUtils.utf8(String.valueOf((char) c2));
			l.add(this.newUnicodeRange(ref, b, b2));
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
					l.add(new PByte(b[i], ref));
				}
				l.add(Expression.newRange(b[b.length - 1] & 0xff, b2[b2.length - 1] & 0xff, ref));
				return Expression.newSequence(l, ref);
			}
		}
	}

	public class _ByteChar extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String t = node.toText();
			if (t.startsWith("U+")) {
				int c = this.parseHexicalNumber(t.charAt(2));
				c = (c * 16) + this.parseHexicalNumber(t.charAt(3));
				c = (c * 16) + this.parseHexicalNumber(t.charAt(4));
				c = (c * 16) + this.parseHexicalNumber(t.charAt(5));
				if (c < 128) {
					return new PByte(c, node);
				}
				String t2 = String.valueOf((char) c);
				return Expression.newString(t2, node);
			}
			int c = this.parseHexicalNumber(t.charAt(t.length() - 2)) * 16
					+ this.parseHexicalNumber(t.charAt(t.length() - 1));
			return new PByte(c, node);
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
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String t = node.toText();
			return this.parseByteClass(t, node);
		}

		private PByteSet parseByteClass(String octet, Object ref) {
			PByteSet b = new PByteSet(ref);
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
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PAny(node);
		}
	}

	public class _Choice extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			List<Expression> l = Expression.newList(node.size());
			for (int i = 0; i < node.size(); i++) {
				Expression.addChoice(l, gamma.newExpression(node.get(i)));
			}
			return Expression.newChoice(l, node);
		}
	}

	public class _Sequence extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			List<Expression> l = Expression.newList(node.size());
			for (int i = 0; i < node.size(); i++) {
				Expression.addSequence(l, gamma.newExpression(node.get(i)));
			}
			return Expression.newSequence(l, node);
		}
	}

	public class _Not extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PNot(gamma.newExpression(node.get(_expr)), node);
		}
	}

	public class _And extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PAnd(gamma.newExpression(node.get(_expr)), node);
		}
	}

	public class _Option extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new POption(gamma.newExpression(node.get(_expr)), node);
		}
	}

	public class _Repetition extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			int min = 0;
			if (node.get(_min, null) != null) {
				min = 1;
			}
			return new PRepetition(gamma.newExpression(node.get(_expr)), min, node);
		}
	}

	// PEG4d TransCapturing

	public class _Tree extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expression.defaultEmpty : gamma.newExpression(exprNode);
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
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expression.defaultEmpty : gamma.newExpression(exprNode);
			return Expression.newFoldTree(GrammarParser.this.parseLabel(node), p, node);
		}
	}

	public class _Link extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PLinkTree(GrammarParser.this.parseLabel(node), gamma.newExpression(node.get(_expr)),
					node);
		}
	}

	public class _Tagging extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PTag(Symbol.unique(node.toText()), node);
		}
	}

	public class _Replace extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PReplace(node.toText(), node);
		}
	}

	public class _If extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PIfCondition(node.getText(_name, ""), node);
		}
	}

	public class _On extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new POnCondition(node.getText(_name, ""), gamma.newExpression(node.get(_expr)), node);
		}
	}

	public class _Block extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PSymbolScope(NezFunc.block, null, gamma.newExpression(node.get(_expr)), node);
		}
	}

	public class _Local extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PSymbolScope(NezFunc.local, Symbol.unique(node.getText(_name, "")),
					gamma.newExpression(node.get(_expr)), node);
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
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getText(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param, node);
			return new PSymbolAction(NezFunc.symbol, param, pat, node);
		}
	}

	public class _Is extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getText(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param, node);
			return new PSymbolPredicate(NezFunc.is, param, pat, node);
		}
	}

	public class _Isa extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getText(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param, node);
			return new PSymbolPredicate(NezFunc.isa, param, pat, node);
		}
	}

	public class _Match extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getText(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param, node);
			return new PSymbolPredicate(NezFunc.match, param, pat, node);
		}
	}

	public class _Exists extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getText(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param, node);
			String symbol = node.getText(_symbol, null);
			if (symbol != null) {
				param = param + "+" + symbol;
			}
			return new PSymbolPredicate(NezFunc.exists, param, pat, node);
		}
	}

	public class _Scanf extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String mask = node.getText(_mask, null);
			return new PScan(mask, gamma.newExpression(node.get(_expr)), node);
		}
	}

	public class _Repeat extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PRepeat(gamma.newExpression(node.get(_expr)), node);
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
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			Tree<?> nameNode = node.get(_name);
			String name = nameNode.toText();
			if (nameNode.is(_String)) {
				name = Production.terminalName(name);
			}
			Expression rule = gamma.grammar.getLocalExpression(name);
			if (rule != null) {
				gamma.logger.reportWarning(node.get(_name), NezFmt.YY0_is_duplicated_name, name);
				return rule;
			}
			rule = gamma.newExpression(node.get(_expr));
			gamma.grammar.addProduction(name, rule);
			boolean isPublic = node.get(_public, null) != null;
			if (isPublic) {
				gamma.grammar.addPublicProduction(name);
			}
			return rule;
		}
	}

	public class _Source extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			for (Tree<?> sub : node) {
				gamma.newExpression(sub);
			}
			return null;
		}
	}

	public class _Grammar extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String name = node.getText(_name, null);
			gamma = gamma.newLocalGramma(node.get(_name), name);
			return gamma.newExpression(node.get(_body));
		}
	}

	public class _Import extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String name = node.getText(_name, null);
			String path = name;
			if (!name.startsWith("/") && !name.startsWith("\\")) {
				path = SourcePosition.extractFilePath(node.getSource().getResourceName()) + "/" + name;
			}
			gamma.importSource(ParserSource.newFileSource(path, null));
			return null;
		}
	}

	public class _Example extends SyntaxRule {
		@Override
		public Expression accept(Gamma e, Tree<?> node) throws IOException {
			return null;
		}
	}

}
