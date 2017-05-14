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

package blue.nez.parser.pasm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import blue.nez.parser.ParserCompiler;
import blue.nez.parser.ParserGrammar;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserOption;
import blue.nez.parser.pasm.PAsmAPI.SymbolDefFunc;
import blue.nez.parser.pasm.PAsmAPI.SymbolResetFunc;
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
import blue.nez.peg.expression.PIf;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PMany;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POn;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.nez.peg.expression.PValue;
import blue.origami.util.OOption;

public class PAsmCompiler implements ParserCompiler {

	public PAsmCompiler() {

	}

	@Override
	public ParserCompiler clone() {
		return new PAsmCompiler();
	}

	// Local Option
	OOption options = null;

	@Override
	public void init(OOption options) {
		this.options = options;
	}

	@Override
	public PAsmCode compile(ParserGrammar grammar) {
		PAsmCode code = new PAsmCode(grammar, this.options);
		new CompilerVisitor(code, grammar, this.options).compileAll();
		return code;
	}

	class CompilerVisitor extends ExpressionVisitor<PAsmInst, PAsmInst> {

		final PAsmCode code;
		final ParserGrammar grammar;

		boolean TreeConstruction = true;
		// boolean BinaryGrammar = false;
		boolean Optimization = false;

		CompilerVisitor(PAsmCode code, ParserGrammar grammar, OOption options) {
			this.code = code;
			this.grammar = grammar;
			// this.BinaryGrammar = grammar.isBinary();
			this.TreeConstruction = options.is(ParserOption.TreeConstruction, true);
		}

		private PAsmCode compileAll() {
			HashSet<PAsmInst> uniq = new HashSet<>();
			PAsmInst ret = new Iret();
			for (Production p : this.grammar) {
				String uname = p.getUniqueName();
				MemoPoint memoPoint = this.code.getMemoPoint(uname);
				PAsmInst prod = this.compileProductionExpression(memoPoint, p.getExpression(), ret);
				this.code.setInstruction(uname, prod);
				PAsmInst block = new Inop(uname, prod);
				this.layoutCode(uniq, this.code.codeList(), block);
			}
			for (PAsmInst inst : this.code.codeList()) {
				if (inst instanceof Icall) {
					Icall call = (Icall) inst;
					if (call.jump == null) {
						call.jump = this.code.getInstruction(call.uname);
					}
				}
			}
			return this.code;
		}

		private PAsmInst compileProductionExpression(MemoPoint memoPoint, Expression p, final PAsmInst ret) {
			assert (ret instanceof Iret);
			if (memoPoint != null) {
				if (memoPoint.typeState == Typestate.Unit) {
					PAsmInst succMemo = this.compile(p, new Mmemo(memoPoint, ret));
					PAsmInst failMemo = new Mmemof(memoPoint);
					PAsmInst memo = new Ialt(succMemo, failMemo);
					return new Mfindpos(memoPoint, memo, ret);
				} else {
					PAsmInst succMemo = this.compile(p, new Mmemo(memoPoint, ret));
					PAsmInst failMemo = new Mmemof(memoPoint);
					PAsmInst memo = new Ialt(succMemo, failMemo);
					return new Mfindtree(memoPoint, memo, ret);
				}
			}
			return this.compile(p, ret);
		}

		private void layoutCode(Set<PAsmInst> uniq, List<PAsmInst> codeList, PAsmInst inst) {
			if (inst == null) {
				return;
			}
			if (!uniq.contains(inst)) {
				uniq.add(inst);
				codeList.add(inst);
				this.layoutCode(uniq, codeList, inst.next);
				for (PAsmInst br : inst.branch()) {
					this.layoutCode(uniq, codeList, br);
				}
			}
		}

		// encoding

		private PAsmInst compile(Expression e, PAsmInst next) {
			return e.visit(this, next);
		}

		@Override
		public PAsmInst visitEmpty(PEmpty p, PAsmInst next) {
			return next;
		}

		private final PAsmInst commonFailure = new Ifail();

		public PAsmInst fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public PAsmInst visitFail(PFail p, PAsmInst next) {
			return this.commonFailure;
		}

		@Override
		public PAsmInst visitByte(PByte p, PAsmInst next) {
			if (p.byteChar() == 0) {
				next = new Pneof(next);
			}
			return new Pbyte(p.byteChar(), next);
		}

		@Override
		public PAsmInst visitByteSet(PByteSet p, PAsmInst next) {
			boolean[] b = p.bools();
			if (b[0]) {
				next = new Pneof(next);
			}
			return new Pset(b, next);
		}

		@Override
		public PAsmInst visitAny(PAny p, PAsmInst next) {
			return new Pany(next);
		}

		@Override
		public final PAsmInst visitNonTerminal(PNonTerminal n, PAsmInst next) {
			Production p = n.getProduction();
			return new Icall(p.getUniqueName(), next);
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
		public final PAsmInst visitOption(POption p, PAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (((PByte) inner).byteChar() != 0) {
						return new Obyte(((PByte) inner).byteChar(), next);
					}
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).bools();
					if (b[0] != true) {
						return new Oset(b, next);
					}
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new Ostr(utf8, next);
				}
			}
			PAsmInst pop = new Isucc(next);
			return new Ialt(this.compile(p.get(0), pop), next);
		}

		@Override
		public PAsmInst visitMany(PMany p, PAsmInst next) {
			PAsmInst next2 = this.compileMany(p, next);
			if (p.isOneMore()) {
				next2 = this.compile(p.get(0), next2);
			}
			return next2;
		}

		private PAsmInst compileMany(PMany p, PAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (((PByte) inner).byteChar() != 0) {
						return new Rbyte(((PByte) inner).byteChar(), next);
					}
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).bools();
					if (b[0] != true) {
						return new Rset(b, next);
					}
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new Rstr(utf8, next);
				}
			}
			PAsmInst skip = new Iupdate();
			PAsmInst start = this.compile(p.get(0), skip);
			skip.next = start;
			return new Ialt(start, next);
		}

		@Override
		public PAsmInst visitAnd(PAnd p, PAsmInst next) {
			PAsmInst inner = this.compile(p.get(0), new Ppop(next));
			return new Ppush(inner);
		}

		@Override
		public final PAsmInst visitNot(PNot p, PAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (((PByte) inner).byteChar() != 0) {
						next = new Pneof(next);
					}
					return new Nbyte(((PByte) inner).byteChar(), next);
				}
				// if (inner instanceof PByteSet) {
				// boolean[] b = ((PByteSet) inner).bools();
				// if (this.BinaryGrammar && b[0] == false) {
				// next = new ASMNeof(next);
				// return new ASMNbinset(b, next);
				// }
				// return new ASMNbset(b, next);
				// }
				if (inner instanceof PAny) {
					return new Nany(next);
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new Nstr(utf8, next);
				}
			}
			PAsmInst fail = new Isucc(new Ifail());
			return new Ialt(this.compile(p.get(0), fail), next);
		}

		@Override
		public PAsmInst visitPair(PPair p, PAsmInst next) {
			PAsmInst nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = this.compile(e, nextStart);
			}
			return nextStart;
		}

		@Override
		public final PAsmInst visitChoice(PChoice p, PAsmInst next) {
			PAsmInst nextChoice = this.compile(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new Ialt(this.compile(e, new Isucc(next)), nextChoice);
			}
			return nextChoice;
		}

		@Override
		public final PAsmInst visitDispatch(PDispatch p, PAsmInst next) {
			PAsmInst[] compiled = new PAsmInst[p.size() + 1];
			compiled[0] = this.commonFailure;
			if (this.isAllD(p)) {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(this.nextD(p.get(i)), next);
				}
				return new Idfa(p.indexMap, compiled);
			} else {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(p.get(i), next);
				}
				return new Idispatch(p.indexMap, compiled);
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
		public PAsmInst visitTree(PTree p, PAsmInst next) {
			if (this.TreeConstruction) {
				next = new Tend(p.tag, p.value, p.endShift, next);
				next = this.compile(p.get(0), next);
				if (p.folding) {
					return new Tfold(p.label, p.beginShift, next);
				} else {
					return new Tbegin(p.beginShift, next);
				}
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PAsmInst visitTag(PTag p, PAsmInst next) {
			if (this.TreeConstruction) {
				return new Ttag(p.tag, next);
			}
			return next;
		}

		@Override
		public PAsmInst visitValue(PValue p, PAsmInst next) {
			if (this.TreeConstruction) {
				return new Tvalue(p.value, next);
			}
			return next;
		}

		// Tree

		@Override
		public final PAsmInst visitLinkTree(PLinkTree p, PAsmInst next) {
			if (this.TreeConstruction) {
				next = new Tlink(p.label, next);
				next = this.compile(p.get(0), next);
				return new Tpush(next);
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PAsmInst visitDetree(PDetree p, PAsmInst next) {
			if (this.TreeConstruction) {
				next = new Tpop(next);
				next = this.compile(p.get(0), next);
				return new Tpush(next);
			}
			return this.compile(p.get(0), next);
		}

		/* Symbol */

		@Override
		public PAsmInst visitSymbolScope(PSymbolScope p, PAsmInst next) {
			if (p.label == null) {
				next = new Spop(next);
				next = this.compile(p.get(0), next);
				return new Spush(next);
			} else {
				next = new Spop(next);
				next = this.compile(p.get(0), next);
				next = new Sdefe(new SymbolResetFunc(), p.label, next);
				return new Spush(next);
			}
		}

		@Override
		public PAsmInst visitSymbolAction(PSymbolAction p, PAsmInst next) {
			return new Ppush(this.compile(p.get(0), new Sdef(new SymbolDefFunc(), p.label, next)));
		}

		@Override
		public PAsmInst visitSymbolPredicate(PSymbolPredicate p, PAsmInst next) {
			if (p.isAndPredicate()) {
				return new Ppush(this.compile(p.get(0), new Spred(p.pred, p.label, next)));
			} else {
				return new Sprede(p.pred, p.label, next);
			}
		}

		@Override
		public PAsmInst visitTrap(PTrap p, PAsmInst next) {
			if (p.trapid != -1) {
				return new Itrap(p.trapid, p.uid, next);
			}
			return next;
		}

		/* Optimization */

		private Expression getInnerExpression(Expression p) {
			return Expression.deref(p.get(0));
		}

		// Unused

		@Override
		public PAsmInst visitIf(PIf e, PAsmInst next) {
			PAsmCompiler.this.options.verbose("unremoved if condition", e);
			return next;
		}

		@Override
		public PAsmInst visitOn(POn e, PAsmInst next) {
			PAsmCompiler.this.options.verbose("unremoved on condition", e);
			return this.compile(e.get(0), next);
		}
	}

}
