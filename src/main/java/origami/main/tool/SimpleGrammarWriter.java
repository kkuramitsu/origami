///***********************************************************************
// * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
// * 
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// ***********************************************************************/
//
//package origami.main.tool;
//
//import origami.main.CommonWriter;
//
//import origami.nez.parser.ParserFactory.GrammarWriter;
//import origami.nez.peg.Grammar;
//import origami.nez.peg.Production;
//
//public class SimpleGrammarWriter extends CommonWriter implements GrammarWriter {
//
//	@Override
//	public void writeGrammar(ParserFactory fac, Grammar grammar) {
//		// OGrammar[] sub = grammar.subGrammars();
//		// for (OGrammar g : sub) {
//		// L("grammar " + g.ns + " ");
//		// Begin("{");
//		// writeGrammar(g);
//		// End("}");
//		// }
//		writeGrammar(grammar);
//	}
//
//	private void writeGrammar(Grammar grammar) {
//		for (Production p : grammar) {
//			String q = p.isPublic() ? "public " : "";
//			L(q + p.getLocalName() + " = " + p.getExpression());
//			this.incIndent();
//			writeProduction(p);
//			this.decIndent();
//		}
//	}
//
//	protected void writeProduction(Production p) {
//		// L("isAlwaysConsumed: " + prop.isAlwaysConsumed(p));
//	}
//
//}
