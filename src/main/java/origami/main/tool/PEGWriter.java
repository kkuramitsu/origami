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
//import origami.nez.peg.Expression.PAnd;
//import origami.nez.peg.Expression.PAny;
//import origami.nez.peg.Expression.PByte;
//import origami.nez.peg.Expression.PByteSet;
//import origami.nez.peg.Expression.PChoice;
//import origami.nez.peg.Expression.PDetree;
//import origami.nez.peg.Expression.PDispatch;
//import origami.nez.peg.Expression.PEmpty;
//import origami.nez.peg.Expression.PFail;
//import origami.nez.peg.Expression.PIfCondition;
//import origami.nez.peg.Expression.PLinkTree;
//import origami.nez.peg.Expression.PNonTerminal;
//import origami.nez.peg.Expression.PNot;
//import origami.nez.peg.Expression.POnCondition;
//import origami.nez.peg.Expression.POption;
//import origami.nez.peg.Expression.PPair;
//import origami.nez.peg.Expression.PRepeat;
//import origami.nez.peg.Expression.PRepetition;
//import origami.nez.peg.Expression.PReplace;
//import origami.nez.peg.Expression.PScan;
//import origami.nez.peg.Expression.PSymbolAction;
//import origami.nez.peg.Expression.PSymbolPredicate;
//import origami.nez.peg.Expression.PSymbolScope;
//import origami.nez.peg.Expression.PTag;
//import origami.nez.peg.Expression.PTrap;
//import origami.nez.peg.Expression.PTree;
//import origami.nez.peg.ExpressionVisitor;
//import origami.nez.peg.Grammar;
//import origami.nez.peg.Production;
//
//public class PEGWriter extends CommonWriter implements GrammarWriter {
//
//	@Override
//	public void writeGrammar(ParserFactory fac, Grammar g) {
//		WriterVisitor v = new WriterVisitor();
//		for (Production p : g.getAllProductions()) {
//			L(p);
//		}
//	}
//
//	class WriterVisitor extends ExpressionVisitor<Void, Void> {
//
//		@Override
//		public Void visitNonTerminal(PNonTerminal e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitEmpty(PEmpty e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitFail(PFail e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitByte(PByte e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitByteSet(PByteSet e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitAny(PAny e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitPair(PPair e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitChoice(PChoice e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitDispatch(PDispatch e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitOption(POption e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitRepetition(PRepetition e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitAnd(PAnd e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitNot(PNot e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitTree(PTree e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitDetree(PDetree e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitLinkTree(PLinkTree e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitTag(PTag e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitReplace(PReplace e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitSymbolScope(PSymbolScope e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitSymbolAction(PSymbolAction e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitSymbolPredicate(PSymbolPredicate e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitIf(PIfCondition e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitOn(POnCondition e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitScan(PScan scanf, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitRepeat(PRepeat e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Void visitTrap(PTrap e, Void a) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//	}
//}
