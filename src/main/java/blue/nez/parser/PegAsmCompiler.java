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

package blue.nez.parser;

import java.util.ArrayList;
import java.util.List;

import blue.nez.parser.ParserContext.SymbolDefinition;
import blue.nez.parser.ParserContext.SymbolReset;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.pegasm.ASMMlookup;
import blue.nez.parser.pegasm.ASMMlookupTree;
import blue.nez.parser.pegasm.ASMMmemoFail;
import blue.nez.parser.pegasm.ASMMmemoSucc;
import blue.nez.parser.pegasm.ASMMmemoTree;
import blue.nez.parser.pegasm.ASMNany;
import blue.nez.parser.pegasm.ASMNbin;
import blue.nez.parser.pegasm.ASMNbinset;
import blue.nez.parser.pegasm.ASMNbset;
import blue.nez.parser.pegasm.ASMNbyte;
import blue.nez.parser.pegasm.ASMNstr;
import blue.nez.parser.pegasm.ASMObin;
import blue.nez.parser.pegasm.ASMObinset;
import blue.nez.parser.pegasm.ASMObset;
import blue.nez.parser.pegasm.ASMObyte;
import blue.nez.parser.pegasm.ASMOstr;
import blue.nez.parser.pegasm.ASMRbin;
import blue.nez.parser.pegasm.ASMRbinset;
import blue.nez.parser.pegasm.ASMRbset;
import blue.nez.parser.pegasm.ASMRbyte;
import blue.nez.parser.pegasm.ASMRstr;
import blue.nez.parser.pegasm.ASMSDec;
import blue.nez.parser.pegasm.ASMSScan;
import blue.nez.parser.pegasm.ASMSbegin;
import blue.nez.parser.pegasm.ASMSdef;
import blue.nez.parser.pegasm.ASMSdef2;
import blue.nez.parser.pegasm.ASMSend;
import blue.nez.parser.pegasm.ASMSpred;
import blue.nez.parser.pegasm.ASMSpred2;
import blue.nez.parser.pegasm.ASMTbegin;
import blue.nez.parser.pegasm.ASMTend;
import blue.nez.parser.pegasm.ASMTfold;
import blue.nez.parser.pegasm.ASMTlink;
import blue.nez.parser.pegasm.ASMTmut;
import blue.nez.parser.pegasm.ASMTpop;
import blue.nez.parser.pegasm.ASMTpush;
import blue.nez.parser.pegasm.ASMTtag;
import blue.nez.parser.pegasm.ASMalt;
import blue.nez.parser.pegasm.ASMany;
import blue.nez.parser.pegasm.ASMback;
import blue.nez.parser.pegasm.ASMbin;
import blue.nez.parser.pegasm.ASMbinset;
import blue.nez.parser.pegasm.ASMbset;
import blue.nez.parser.pegasm.ASMbyte;
import blue.nez.parser.pegasm.ASMcall;
import blue.nez.parser.pegasm.ASMdfa;
import blue.nez.parser.pegasm.ASMdispatch;
import blue.nez.parser.pegasm.ASMfail;
import blue.nez.parser.pegasm.ASMnop;
import blue.nez.parser.pegasm.ASMpos;
import blue.nez.parser.pegasm.ASMret;
import blue.nez.parser.pegasm.ASMstep;
import blue.nez.parser.pegasm.ASMsucc;
import blue.nez.parser.pegasm.ASMtrap;
import blue.nez.parser.pegasm.PegAsm;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Production;
import blue.nez.peg.Typestate;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;

public class PegAsmCompiler implements ParserCompiler {

	public PegAsmCompiler() {

	}

	@Override
	public ParserCompiler clone() {
		return new PegAsmCompiler();
	}

	// Local Option
	OOption options = null;
	boolean enableMemo = false;

	@Override
	public void init(OOption options) {
		this.options = options;
		this.enableMemo = options.is(ParserOption.PackratParsing, true);
	}

	@Override
	public PegAsmCode compile(ParserGrammar grammar) {
		PegAsmCode code = new PegAsmCode(grammar, this.options);
		if (this.enableMemo) {
			code.initMemoPoint();
		}
		new CompilerVisitor(code, grammar, this.options).compileAll();
		return code;
	}

	class CompilerVisitor extends ExpressionVisitor<PegAsmInst, PegAsmInst> {

		final PegAsmCode code;
		final ParserGrammar grammar;

		boolean TreeConstruction = true;
		boolean enableMemo = false;
		boolean BinaryGrammar = false;
		boolean Optimization = true;

		CompilerVisitor(PegAsmCode code, ParserGrammar grammar, OOption options) {
			this.code = code;
			this.grammar = grammar;
			this.BinaryGrammar = grammar.isBinary();
			this.TreeConstruction = options.is(ParserOption.TreeConstruction, true);
			this.enableMemo = options.is(ParserOption.PackratParsing, true);

		}

		private PegAsmCode compileAll() {
			for (Production p : this.grammar) {
				this.compileProduction(this.code.codeList(), p, new ASMret());
			}
			for (PegAsmInst inst : this.code.codeList()) {
				if (inst instanceof ASMcall) {
					ASMcall call = (ASMcall) inst;
					if (call.jump == null) {
						call.jump = call.next;
						call.next = PegAsm.joinPoint(this.code.getInstruction(call.uname));// f.getCompiled();
					}
				}
			}
			return this.code;
		}

		protected void compileProduction(List<PegAsmInst> codeList, Production p, PegAsmInst next) {
			MemoPoint memoPoint = this.code.getMemoPoint(p.getUniqueName());
			next = this.compileProductionExpression(memoPoint, p.getExpression(), next);
			this.code.setInstruction(p.getUniqueName(), next);
			PegAsmInst block = new ASMnop(p.getUniqueName(), next);
			this.layoutCode(codeList, block);
		}

		private PegAsmInst compileProductionExpression(MemoPoint memoPoint, Expression p, PegAsmInst next) {
			if (memoPoint != null) {
				if (memoPoint.typeState == Typestate.Unit) {
					PegAsmInst memo = new ASMMmemoSucc(memoPoint, next);
					PegAsmInst inside = this.compile(p, memo);
					PegAsmInst failmemo = new ASMMmemoFail(memoPoint);
					inside = new ASMalt(failmemo, inside);
					return new ASMMlookup(memoPoint, inside, next);
				} else {
					PegAsmInst memo = new ASMMmemoTree(memoPoint, next);
					PegAsmInst inside = this.compile(p, memo);
					PegAsmInst failmemo = new ASMMmemoFail(memoPoint);
					inside = new ASMalt(failmemo, inside);
					return new ASMMlookupTree(memoPoint, inside, next);
				}
			}
			return this.compile(p, next);
		}

		private void layoutCode(List<PegAsmInst> codeList, PegAsmInst inst) {
			if (inst == null) {
				return;
			}
			if (inst.id == -1) {
				inst.id = codeList.size();
				codeList.add(inst);
				this.layoutCode(codeList, inst.next);
				// if (inst.next != null && inst.id + 1 != inst.next.id) {
				// MozInst.joinPoint(inst.next);
				// }
				this.layoutCode(codeList, inst.branch());
				if (inst instanceof ASMdispatch) {
					ASMdispatch match = (ASMdispatch) inst;
					for (int ch = 0; ch < match.jumpTable.length; ch++) {
						this.layoutCode(codeList, match.jumpTable[ch]);
					}
				}
			}
		}

		// encoding

		private PegAsmInst compile(Expression e, PegAsmInst next) {
			return e.visit(this, next);
		}

		@Override
		public PegAsmInst visitEmpty(PEmpty p, PegAsmInst next) {
			return next;
		}

		private final PegAsmInst commonFailure = new ASMfail();

		public PegAsmInst fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public PegAsmInst visitFail(PFail p, PegAsmInst next) {
			return this.commonFailure;
		}

		@Override
		public PegAsmInst visitByte(PByte p, PegAsmInst next) {
			if (/* this.BinaryGrammar && */ p.byteChar() == 0) {
				return new ASMbin(next);
			}
			return new ASMbyte(p.byteChar(), next);
		}

		@Override
		public PegAsmInst visitByteSet(PByteSet p, PegAsmInst next) {
			boolean[] b = p.bools();
			if (this.BinaryGrammar && b[0]) {
				return new ASMbinset(b, next);
			}
			b[0] = false;
			return new ASMbset(b, next);
		}

		@Override
		public PegAsmInst visitAny(PAny p, PegAsmInst next) {
			return new ASMany(next);
		}

		@Override
		public final PegAsmInst visitNonTerminal(PNonTerminal n, PegAsmInst next) {
			Production p = n.getProduction();
			return new ASMcall(p.getUniqueName(), next);
		}

		private byte[] toMultiChar(Expression e) {
			ArrayList<Integer> l = new ArrayList<>();
			Expression.extractMultiBytes(e, l);
			byte[] utf8 = new byte[l.size()];
			for (int i = 0; i < l.size(); i++) {
				utf8[i] = (byte) (int) l.get(i);
			}
			return utf8;
		}

		@Override
		public final PegAsmInst visitOption(POption p, PegAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() == 0) {
						return new ASMObin(next);
					}
					return new ASMObyte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).bools();
					if (this.BinaryGrammar && b[0]) {
						return new ASMObinset(b, next);
					}
					b[0] = false;
					return new ASMObset(b, next);
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMOstr(utf8, next);
				}
			}
			PegAsmInst pop = new ASMsucc(next);
			return new ASMalt(next, this.compile(p.get(0), pop));
		}

		@Override
		public PegAsmInst visitRepetition(PRepetition p, PegAsmInst next) {
			PegAsmInst next2 = this.compileRepetition(p, next);
			if (p.isOneMore()) {
				next2 = this.compile(p.get(0), next2);
			}
			return next2;
		}

		private PegAsmInst compileRepetition(PRepetition p, PegAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() == 0) {
						return new ASMRbin(next);
					}
					return new ASMRbyte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).bools();
					if (this.BinaryGrammar && b[0]) {
						return new ASMRbinset(b, next);
					}
					b[0] = false;
					return new ASMRbset(b, next);
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMRstr(utf8, next);
				}
			}
			PegAsmInst skip = new ASMstep();
			PegAsmInst start = this.compile(p.get(0), skip);
			skip.next = PegAsm.joinPoint(start);
			return new ASMalt(next, start);
		}

		@Override
		public PegAsmInst visitAnd(PAnd p, PegAsmInst next) {
			PegAsmInst inner = this.compile(p.get(0), new ASMback(next));
			return new ASMpos(inner);
		}

		@Override
		public final PegAsmInst visitNot(PNot p, PegAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() != 0) {
						return new ASMNbin(((PByte) inner).byteChar(), next);
					}
					return new ASMNbyte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).bools();
					if (this.BinaryGrammar && b[0] == false) {
						return new ASMNbinset(b, next);
					}
					return new ASMNbset(b, next);
				}
				if (inner instanceof PAny) {
					return new ASMNany(next);
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMNstr(utf8, next);
				}
			}
			PegAsmInst fail = new ASMsucc(new ASMfail());
			return new ASMalt(next, this.compile(p.get(0), fail));
		}

		@Override
		public PegAsmInst visitPair(PPair p, PegAsmInst next) {
			PegAsmInst nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = this.compile(e, nextStart);
			}
			return nextStart;
		}

		@Override
		public final PegAsmInst visitChoice(PChoice p, PegAsmInst next) {
			PegAsmInst nextChoice = this.compile(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new ASMalt(nextChoice, this.compile(e, new ASMsucc(next)));
			}
			return nextChoice;
		}

		@Override
		public final PegAsmInst visitDispatch(PDispatch p, PegAsmInst next) {
			PegAsmInst[] compiled = new PegAsmInst[p.size() + 1];
			compiled[0] = this.commonFailure;
			if (this.isAllD(p)) {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(this.nextD(p.get(i)), next);
				}
				return new ASMdfa(p.indexMap, compiled);
			} else {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(p.get(i), next);
				}
				return new ASMdispatch(p.indexMap, compiled);
			}
		}

		private boolean isAllD(PDispatch p) {
			for (int i = 0; i < p.size(); i++) {
				if (!this.isD(p.get(i))) {
					return false;
				}
			}
			return true;
		}

		private boolean isD(Expression e) {
			if (e instanceof PPair) {
				if (e.get(0) instanceof PAny) {
					return true;
				}
				return false;
			}
			return (e instanceof PAny);
		}

		private Expression nextD(Expression e) {
			if (e instanceof PPair) {
				return e.get(1);
			}
			return Expression.defaultEmpty;
		}

		@Override
		public PegAsmInst visitTree(PTree p, PegAsmInst next) {
			if (this.TreeConstruction) {
				next = new ASMTend(p.tag, p.value, p.endShift, next);
				next = this.compile(p.get(0), next);
				if (p.folding) {
					// System.out.println("@@@ folding" + p);
					return new ASMTfold(p.label, p.beginShift, next);
				} else {
					return new ASMTbegin(p.beginShift, next);
				}
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PegAsmInst visitTag(PTag p, PegAsmInst next) {
			if (this.TreeConstruction) {
				return new ASMTtag(p.tag, next);
			}
			return next;
		}

		@Override
		public PegAsmInst visitReplace(PReplace p, PegAsmInst next) {
			if (this.TreeConstruction) {
				return new ASMTmut(p.value, next);
			}
			return next;
		}

		// Tree

		@Override
		public final PegAsmInst visitLinkTree(PLinkTree p, PegAsmInst next) {
			if (this.TreeConstruction) {
				next = new ASMTlink(p.label, next);
				next = this.compile(p.get(0), next);
				return new ASMTpush(next);
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PegAsmInst visitDetree(PDetree p, PegAsmInst next) {
			if (this.TreeConstruction) {
				next = new ASMTpop(next);
				next = this.compile(p.get(0), next);
				return new ASMTpush(next);
			}
			return this.compile(p.get(0), next);
		}

		/* Symbol */

		@Override
		public PegAsmInst visitSymbolScope(PSymbolScope p, PegAsmInst next) {
			if (p.label == null) {
				next = new ASMSend(next);
				next = this.compile(p.get(0), next);
				return new ASMSbegin(next);
			} else {
				next = new ASMSend(next);
				next = this.compile(p.get(0), next);
				next = new ASMSdef2(new SymbolReset(), p.label, null, next);
				return new ASMSbegin(next);
			}
		}

		@Override
		public PegAsmInst visitSymbolAction(PSymbolAction p, PegAsmInst next) {
			return new ASMpos(this.compile(p.get(0), new ASMSdef(new SymbolDefinition(), p.label, null, next)));
		}

		@Override
		public PegAsmInst visitSymbolPredicate(PSymbolPredicate p, PegAsmInst next) {
			if (p.isEmpty()) {
				if (p.thunk == null) {
					return new ASMSpred2(p.pred, p.label, null, next);
				} else {
					return new ASMSpred2(p.pred, p.label, OStringUtils.utf8(p.thunk.toString()), next);
				}
			} else {
				return new ASMpos(this.compile(p.get(0), new ASMSpred(p.pred, p.label, next)));
			}
		}

		@Override
		public PegAsmInst visitScan(PScan p, PegAsmInst next) {
			return new ASMpos(this.compile(p.get(0), new ASMSScan(p.mask, p.shift, next)));
		}

		@Override
		public PegAsmInst visitRepeat(PRepeat p, PegAsmInst next) {
			PegAsmInst check = new ASMSDec(next, null);
			PegAsmInst repeated = this.compile(p.get(0), check);
			check.next = repeated;
			return check;
		}

		@Override
		public PegAsmInst visitTrap(PTrap p, PegAsmInst next) {
			if (p.trapid != -1) {
				return new ASMtrap(p.trapid, p.uid, next);
			}
			return next;
		}

		/* Optimization */

		private Expression getInnerExpression(Expression p) {
			return Expression.deref(p.get(0));
		}

		// Unused

		@Override
		public PegAsmInst visitIf(PIfCondition e, PegAsmInst next) {
			PegAsmCompiler.this.options.verbose("unremoved if condition", e);
			return next;
		}

		@Override
		public PegAsmInst visitOn(POnCondition e, PegAsmInst next) {
			PegAsmCompiler.this.options.verbose("unremoved on condition", e);
			return this.compile(e.get(0), next);
		}
	}

}
