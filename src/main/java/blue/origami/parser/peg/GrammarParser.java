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

package blue.origami.parser.peg;

import java.io.IOException;
import java.util.List;

import blue.origami.common.OOption;
import blue.origami.common.OSource;
import blue.origami.common.OStringUtils;
import blue.origami.common.SourceLogger;
import blue.origami.common.SourcePosition;
import blue.origami.common.Symbol;
import blue.origami.common.Tree;
import blue.origami.common.TreeVisitorMap;
import blue.origami.parser.Parser;
import blue.origami.parser.ParserSource;
import blue.origami.parser.pasm.PAsmAPI.SymbolContainsFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolDecFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolDefFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolEqualsFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolExistFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolExistString;
import blue.origami.parser.pasm.PAsmAPI.SymbolMatchFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolScanBitFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolScanFunc;
import blue.origami.parser.pasm.PAsmAPI.SymbolZeroFunc;

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
		new OPegGrammar().load(OPegGrammar, "Start", options);
		OPegParser = OPegGrammar.newParser(options);
	}

	class Gamma {
		final SourceLogger logger;
		final Grammar grammar;

		Gamma(Grammar grammar, SourceLogger logger) {
			this.grammar = grammar;
			this.logger = logger == null ? new SourceLogger.SimpleSourceLogger() : logger;
		}

		public Expression parse(Tree<?> node) throws IOException {
			Expression e = GrammarParser.this.find(key(node)).accept(this, node);
			if (e != null) {
				e = e.setSourcePosition(node);
			}
			return e;
		}

		public Gamma newLocalGramma(SourcePosition s, String name) {
			Grammar g = this.grammar.getGrammar(name);
			if (g != null) {
				this.logger.reportWarning(s, NezFmt.YY1_is_duplicated_name, name);
			} else {
				g = this.grammar.newLocalGrammar(name);
			}
			return new Gamma(g, this.logger);
		}

		public void importSource(OSource s) throws IOException {
			Tree<?> t = OPegParser.parse(s);
			GrammarParser.this.find(key(t)).accept(this, t);
		}

	}

	public void importSource(Grammar base, OSource s) throws IOException {
		Gamma gamma = new Gamma(base, null);
		gamma.importSource(s);
	}

	private static String key(Tree<?> node) {
		return node.getTag().getSymbol();
	}

	public class SyntaxRule implements ExpressionTransducer {
		@Override
		public Expression accept(Gamma e, Tree<?> node) throws IOException {
			throw new IOException("undefined parsing expression: " + node.getString());
			// return Expression.defaultFailure;
		}
	}

	/* Expression */

	public class _NonTerminal extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String ns = null;
			String name = node.getString();
			int loc = name.indexOf('.');
			if (loc > 0) {
				ns = name.substring(0, loc);
				name = name.substring(loc + 1);
			}
			return new PNonTerminal(gamma.grammar, ns, name);
		}
	}

	public class _String extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String name = Production.terminalName(node.getString());
			return new PNonTerminal(gamma.grammar, name);
		}
	}

	public class _Character extends SyntaxRule {
		@Override
		public Expression accept(Gamma e, Tree<?> node) throws IOException {
			return Expression.newString(OStringUtils.unquoteString(node.getString()));
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
						l.add(this.newCharSet(o.getStringAt(0, ""), o.getStringAt(1, "")));
					} else { // single
						l.add(this.newCharSet(o.getString(), o.getString()));
					}
				}
			}
			return Expression.newChoice(l);
		}

		private Expression newCharSet(String t, String t2) {
			int c = OStringUtils.parseAscii(t);
			int c2 = OStringUtils.parseAscii(t2);
			if (c != -1 && c2 != -1) {
				return Expression.newRange(c, c2);
			}
			c = OStringUtils.parseUnicode(t);
			c2 = OStringUtils.parseUnicode(t2);
			if (c < 128 && c2 < 128) {
				return Expression.newRange(c, c2);
			} else {
				return this.newUnicodeRange(c, c2);
			}
		}

		private Expression newUnicodeRange(int c, int c2) {
			byte[] b = OStringUtils.utf8(String.valueOf((char) c));
			byte[] b2 = OStringUtils.utf8(String.valueOf((char) c2));
			if (this.equalsBase(b, b2)) {
				return this.newUnicodeRange(b, b2);
			}
			List<Expression> l = Expression.newList(b.length);
			b2 = b;
			for (int pc = c + 1; pc <= c2; pc++) {
				byte[] b3 = OStringUtils.utf8(String.valueOf((char) pc));
				if (this.equalsBase(b, b3)) {
					b2 = b3;
					continue;
				}
				l.add(this.newUnicodeRange(b, b2));
				b = b3;
				b2 = b3;
			}
			b2 = OStringUtils.utf8(String.valueOf((char) c2));
			l.add(this.newUnicodeRange(b, b2));
			return Expression.newChoice(l);
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

		private Expression newUnicodeRange(byte[] b, byte[] b2) {
			if (b[b.length - 1] == b2[b.length - 1]) {
				return Expression.newString(b);
			} else {
				List<Expression> l = Expression.newList(b.length);
				for (int i = 0; i < b.length - 1; i++) {
					l.add(new PByte(b[i]));
				}
				l.add(Expression.newRange(b[b.length - 1] & 0xff, b2[b2.length - 1] & 0xff));
				return Expression.newSequence(l);
			}
		}
	}

	public class _ByteChar extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String t = node.getString();
			if (t.startsWith("U+")) {
				int c = this.parseHexicalNumber(t.charAt(2));
				c = (c * 16) + this.parseHexicalNumber(t.charAt(3));
				c = (c * 16) + this.parseHexicalNumber(t.charAt(4));
				c = (c * 16) + this.parseHexicalNumber(t.charAt(5));
				if (c < 128) {
					return new PByte(c);
				}
				String t2 = String.valueOf((char) c);
				return Expression.newString(t2);
			}
			int c = this.parseHexicalNumber(t.charAt(t.length() - 2)) * 16
					+ this.parseHexicalNumber(t.charAt(t.length() - 1));
			return new PByte(c);
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
			String t = node.getString();
			return this.parseByteClass(t);
		}

		private PByteSet parseByteClass(String octet) {
			PByteSet b = new PByteSet();
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
			return new PAny();
		}
	}

	public class _UChoice extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			List<Expression> l = Expression.newList(node.size());
			for (int i = 0; i < node.size(); i++) {
				Expression.addChoice(l, gamma.parse(node.get(i)));
			}
			return Expression.newChoice(l);
		}
	}

	public class _Choice extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			List<Expression> l = Expression.newList(node.size());
			for (int i = 0; i < node.size(); i++) {
				Expression.addChoice(l, gamma.parse(node.get(i)));
			}
			return Expression.newChoice(l);
		}
	}

	public class _Sequence extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			List<Expression> l = Expression.newList(node.size());
			for (int i = 0; i < node.size(); i++) {
				Expression.addSequence(l, gamma.parse(node.get(i)));
			}
			return Expression.newSequence(l);
		}
	}

	public class _Not extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PNot(gamma.parse(node.get(_expr)));
		}
	}

	public class _And extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PAnd(gamma.parse(node.get(_expr)));
		}
	}

	public class _Option extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new POption(gamma.parse(node.get(_expr)));
		}
	}

	public class _Repetition extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			int min = 0;
			if (node.get(_min, null) != null) {
				min = 1;
			}
			return new PMany(gamma.parse(node.get(_expr)), min);
		}
	}

	// PEG4d TransCapturing

	public class _Tree extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expression.defaultEmpty : gamma.parse(exprNode);
			return Expression.newTree(p);
		}
	}

	private Symbol parseLabel(Tree<?> node) {
		Symbol label = null;
		Tree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = Symbol.unique(labelNode.getString());
		}
		return label;
	}

	public class _FoldTree extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expression.defaultEmpty : gamma.parse(exprNode);
			return Expression.newFoldTree(GrammarParser.this.parseLabel(node), p);
		}
	}

	public class _Link extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PLinkTree(GrammarParser.this.parseLabel(node), gamma.parse(node.get(_expr)));
		}
	}

	public class _Tagging extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PTag(Symbol.unique(node.getString()));
		}
	}

	public class _Replace extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PValue(node.getString());
		}
	}

	public class _If extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PIf(node.getStringAt(_name, ""));
		}
	}

	public class _On extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new POn(node.getStringAt(_name, ""), gamma.parse(node.get(_expr)));
		}
	}

	public class _Block extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PSymbolScope(gamma.parse(node.get(_expr)));
		}
	}

	public class _Local extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			return new PSymbolScope(Symbol.unique(node.getStringAt(_name, "")), gamma.parse(node.get(_expr)));
		}
	}

	// public class _Def extends TreeVisitor {
	// @Override
	// public Expression accept(Tree<?> node, Expression e) {
	// Grammar g = getGrammar();
	// Tree<?> nameNode = node.get(_name);
	// Expression.NonTerminal pat = new Expression.NonTerminal(g,
	// nameNode.toText());
	// Expression expr = newInstance(node.get(_expr));
	// Production p = g.addProduction(pat.getLocalName(), expr);
	// reportWarning(nameNode, "new production generated: " + p);
	// return Expressions.newSymbol(node, pat);
	// }
	// }

	public class _Symbol extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getStringAt(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param);
			pat.setSourcePosition(node.get(_name));
			return new PSymbolAction(new SymbolDefFunc(), param, pat);
		}
	}

	public class _Is extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getStringAt(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param);
			pat.setSourcePosition(node.get(_name));
			return new PSymbolPredicate(new SymbolEqualsFunc(), true, param, pat);
		}
	}

	public class _Isa extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getStringAt(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param);
			pat.setSourcePosition(node.get(_name));
			return new PSymbolPredicate(new SymbolContainsFunc(), true, param, pat);
		}
	}

	public class _Match extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getStringAt(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param);
			pat.setSourcePosition(node.get(_name));
			return new PSymbolPredicate(new SymbolMatchFunc(), false, param, pat);
		}
	}

	public class _Exists extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getStringAt(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param);
			pat.setSourcePosition(node.get(_name));
			String symbol = node.getStringAt(_symbol, null);
			if (symbol != null) {
				return new PSymbolPredicate(new SymbolExistString(symbol), false, param, pat);
			}
			return new PSymbolPredicate(new SymbolExistFunc(), false, param, pat);
		}
	}

	public class _Scanf extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String param = node.getStringAt(_name, "");
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param);
			pat.setSourcePosition(node.get(_name));
			String mask = node.getStringAt(_mask, null);
			if (mask != null) {
				long bits = Long.parseUnsignedLong(mask, 2);
				int shift = 0;
				long m = bits;
				while ((m & 1L) == 0) {
					m >>= 1;
					shift++;
				}
				return new PSymbolAction(new SymbolScanBitFunc(bits, shift), param, pat);
			}
			return new PSymbolAction(new SymbolScanFunc(), param, pat);
		}
	}

	public class _Repeat extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			Expression e = gamma.parse(node.get(_expr));
			String param = node.getStringAt(_name, null);
			if (param == null) {
				return new PMany(e, 0);
			}
			PNonTerminal pat = new PNonTerminal(gamma.grammar, param);
			pat.setSourcePosition(node.get(_name));
			e = e.cat(new PSymbolPredicate(new SymbolDecFunc(), false, param, pat));
			e = new PMany(e, 0).cat(new PSymbolPredicate(new SymbolZeroFunc(), false, param, pat));
			return e;
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
			String name = nameNode.getString();
			if (nameNode.is(_String)) {
				name = Production.terminalName(name);
			}
			Expression rule = gamma.grammar.getLocalExpression(name);
			if (rule != null) {
				gamma.logger.reportWarning(node.get(_name), NezFmt.YY1_is_duplicated_name, name);
				return rule;
			}
			rule = gamma.parse(node.get(_expr));
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
				gamma.parse(sub);
			}
			return null;
		}
	}

	public class _Grammar extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String name = node.getStringAt(_name, null);
			gamma = gamma.newLocalGramma(node.get(_name), name);
			return gamma.parse(node.get(_body));
		}
	}

	public class _Import extends SyntaxRule {
		@Override
		public Expression accept(Gamma gamma, Tree<?> node) throws IOException {
			String name = node.getStringAt(_name, null);
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
