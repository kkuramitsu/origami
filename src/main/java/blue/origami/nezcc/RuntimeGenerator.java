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

package blue.origami.nezcc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import blue.origami.nez.parser.ParserGrammar;
import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.expression.ByteSet;
import blue.origami.nez.peg.expression.PAny;
import blue.origami.nez.peg.expression.PMany;

public abstract class RuntimeGenerator<B, C> extends CodeSection<C> {

	public interface FuncGen {
		public void gen();
	}

	public interface ThunkGen {
		public void gen(Object thunk);
	}

	private HashMap<String, Object> genMap = new HashMap<>();
	private HashMap<String, String[]> depsMap = new HashMap<>();
	private HashMap<String, FuncGen> funcMap = new HashMap<>();
	private HashMap<String, ThunkGen> thunkMap = new HashMap<>();

	boolean hasLib(String lib) {
		return this.genMap.containsKey(lib);
	}

	void definedLib(String lib) {
		this.genMap.put(lib, true);
	}

	void defineLib(String name, FuncGen gen) {
		String[] deps = name.split(",");
		this.funcMap.put(deps[0], gen);
		if (deps.length > 1) {
			this.depsMap.put(deps[0], deps);
		}
	}

	String makeLib(String name) {
		if (!this.hasLib(name)) {
			this.definedLib(name);
			if (!this.funcMap.containsKey(name)) {
				System.out.println("undefined function: " + name);
				return name;
			}
			SourceSection sec = this.openSection(this.RuntimeLibrary);
			this.funcMap.get(name).gen();
			this.closeSection(sec);
		}
		return name;
	}

	void defineLib2(String name, ThunkGen gen) {
		String[] deps = name.split(",");
		this.thunkMap.put(deps[0], gen);
		if (deps.length > 1) {
			this.depsMap.put(deps[0], deps);
		}
	}

	String makeLib(String name, Object thunk) {
		String lib = name + thunk;
		if (!this.hasLib(lib)) {
			this.definedLib(lib);
			if (!this.thunkMap.containsKey(name)) {
				System.out.println("undefined thunk: " + name);
				return lib;
			}
			this.thunkMap.get(name).gen(thunk);
			SourceSection sec = this.openSection(this.RuntimeLibrary);
			if (!this.funcMap.containsKey(lib)) {
				System.out.println("undefined thunk: " + name + ", lib=" + lib);
				return lib;
			}
			this.funcMap.get(lib).gen();
			this.closeSection(sec);
		}
		return lib;
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String[] params, Block<C> block) {
		pg.declFunc(ret, funcName, params, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, Block<C> block) {
		if (a0.indexOf(",") > 0) {
			this.defFunc(pg, ret, funcName, a0.split(","), block);
		} else {
			this.defFunc(pg, ret, funcName, new String[] { a0 }, block);
		}
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1,
			Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1, String a2,
			Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1, String a2,
			String a3, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2, a3 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1, String a2,
			String a3, String a4, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2, a3, a4 }, block);
	}

	void loadContext(ParserGenerator<B, C> pg, ParserGrammar g) {
		this.defineLib("TreeFunc", () -> {
			pg.declFuncType(this.T("tree"), "TreeFunc", "tag", "inputs", "pos", "epos", "cnt");
		});
		this.defineLib("TreeSetFunc", () -> {
			pg.declFuncType(this.T("tree"), "TreeSetFunc", "tree", "cnt", "label", "child");
		});
		this.defineLib("ParserFunc", () -> {
			pg.declFuncType(this.T("matched"), "ParserFunc", "px");
		});
		this.defineLib("getbyte", () -> {
			this.makeLib("move");
			this.defFunc(pg, this.T("c"), "getbyte", "px", () -> {
				C expr = pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos"));
				return (pg.emitUnsigned(expr));
			});
		});
		this.defineLib("nextbyte", () -> {
			this.defFunc(pg, this.T("c"), "nextbyte", "px", () -> {
				C inc = pg.emitInc(pg.emitGetter("px.pos"));
				if (inc != null) {
					C expr = pg.emitArrayIndex(pg.emitGetter("px.inputs"), inc);
					return ((pg.emitUnsigned(expr)));
				} else {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "c",
							pg.emitUnsigned(pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos"))));
					pg.Setter(block, "px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.vInt(1)));
					pg.emitStmt(block, pg.emitReturn(pg.V("c")));
					return (pg.endBlock(block));
				}
			});
		});
		this.defineLib("move", () -> {
			this.defFunc(pg, this.T("matched"), "move", "px", "shift", () -> {
				B block = pg.beginBlock();
				pg.Setter(block, "px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return (pg.endBlock(block));
			});
		});
		this.defineLib("neof", () -> {
			this.defFunc(pg, this.T("matched"), "neof", "px", () -> {
				C length = pg.emitGetter("px.length");
				return (pg.emitOp(pg.emitGetter("px.pos"), "<", length));
			});
		});
		this.defineLib("next1", () -> {
			this.makeLib("nextbyte");
			this.defFunc(pg, this.T("matched"), "next1", "px", "c", () -> {
				C expr = pg.emitFunc("nextbyte", pg.V("px"));
				expr = pg.emitOp(expr, "==", pg.V("c"));
				return (expr);
			});
		});
		/* backpos(px, pos) */
		this.defineLib("backpos", () -> {
			this.defFunc(pg, this.T("pos"), "backpos", "px", "pos", () -> {
				B block = pg.beginBlock();
				pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.head_pos"), "<", pg.V("pos")), false, () -> {
					return pg.emitSetter("px.head_pos", pg.V("pos"));
				});
				pg.Return(block, pg.V("pos"));
				return pg.endBlock(block);
			});
		});
		this.defineLib("fTrue", () -> {
			this.defFunc(pg, pg.T("matched"), "fTrue", "px", () -> {
				return pg.emitSucc();
			});
		});
		this.defineLib("fFalse", () -> {
			this.defFunc(pg, pg.T("matched"), "fFalse", "px", () -> {
				return pg.emitFail();
			});
		});

		this.defineLib2("many", (Object thunk) -> {
			PMany e = (PMany) thunk;
			Expression p = Expression.deref(e.get(0));
			ByteSet bs = Expression.getByteSet(p, pg.isBinary());
			if (bs != null) {
				String pname = bs.getUnsignedByte() == -1 ? "s" : "c";
				C inner = pg.emitMatchByteSet(bs, pg.V(pname), false);
				String fname = "many" + pname + (bs.is(0) ? "0" : "");
				this.defineLib(fname, () -> {
					this.defFunc(pg, pg.T("matched"), fname, "px", pname, () -> {
						B block = pg.beginBlock();
						pg.emitWhileStmt(block, inner, () -> {
							return pg.emitMove(pg.vInt(1));
						});
						pg.Return(block, pg.emitSucc());
						return pg.endBlock(block);
					});
				});
			}
			// C arg = bs.getUnsignedByte() == -1 ? pg.vByteSet(bs) :
			// pg.emitChar(bs.getUnsignedByte());
			// C expr = pg.emitFunc(fname, pg.V("px"), arg);
			// if (e.isOneMore()) {
			// expr = pg.emitAnd(pg.emitMatchByteSet(bs), expr);
			// }
			// return expr;
			// }
			// return null;

		});

		this.defineLib2("option", (Object thunk) -> {

		});

		this.defineLib("NezParserContext", () -> {
			List<String> fields = new ArrayList<>();
			fields.add("inputs");
			fields.add("length");
			fields.add("pos");
			fields.add("head_pos");
			fields.add("tree");
			this.makeLib("TreeLog");
			fields.add("treeLog");
			this.makeLib("TreeFunc");
			fields.add("newFunc");
			if (!pg.isFunctional()) {
				this.makeLib("TreeSetFunc");
				fields.add("setFunc");
			}
			// if (pg.isStateful()) {
			this.makeLib("State");
			fields.add("state");
			// }
			// if (g.getMemoPointSize() > 0) {
			this.makeLib("MemoEntry");
			fields.add("memos");
			// }
			pg.declStruct("NezParserContext", fields.toArray(new String[fields.size()]));
			pg.makeLib("newMemos");
		});

	}

	C makeOptionInlineCall(ParserGenerator<B, C> pg, Expression e) {
		Expression p = Expression.deref(e.get(0));
		ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		if (bs != null) {
			String pname = bs.getUnsignedByte() == -1 ? "s" : "c";
			C inner = pg.emitMatchByteSet(bs, pg.V(pname), false);
			String fname = "option" + pname + (bs.is(0) ? "0" : "");
			this.defFunc(pg, pg.T("matched"), fname, "px", pname, () -> {
				B block = pg.beginBlock();
				pg.emitIfStmt(block, inner, false, () -> {
					return pg.emitMove(pg.vInt(1));
				});
				pg.Return(block, pg.emitSucc());
				return pg.endBlock(block);
			});
			C arg = bs.getUnsignedByte() == -1 ? pg.vByteSet(bs) : pg.emitChar(bs.getUnsignedByte());
			return pg.emitFunc(fname, pg.V("px"), arg);
		}
		return null;
	}

	C makeAndInlineCall(ParserGenerator<B, C> pg, Expression e) {
		Expression p = Expression.deref(e.get(0));
		if (p instanceof PAny) {
			return pg.emitFunc("neof", pg.V("px"));
		}
		ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		if (bs != null) {
			return pg.emitMatchByteSet(bs, null, false);
		}
		return null;
	}

	C makeNotInlineCall(ParserGenerator<B, C> pg, Expression e) {
		Expression p = Expression.deref(e.get(0));
		if (p instanceof PAny) {
			return pg.emitNot(pg.emitFunc("neof", pg.V("px")));
		}
		ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		if (bs != null) {
			bs = bs.not(pg.isBinary());
			C expr = pg.emitMatchByteSet(bs, null, false);
			return expr;
		}
		return null;
	}

	/* TreeLog */

	private boolean useLinkList() {
		return true;
	}

	// String nullCheck(String t, String v) {
	// return t == null ? v : t;
	//

	private C topos(ParserGenerator<B, C> pg, String name) {
		C expr = pg.V(name);
		if (pg.usePointerPosition()) {
			return pg.emitCast("pos", expr);
		}
		return expr;
	}

	private C frompos(ParserGenerator<B, C> pg, C expr) {
		if (pg.usePointerPosition()) {
			return pg.emitCast("ntag", expr);
		}
		return expr;
	}

	private C emitNewTreeLog(ParserGenerator<B, C> pg, C op, C pos, C tree, C prevLog) {
		List<C> args = new ArrayList<>();
		args.add(op == null ? pg.vInt(0) : op);
		args.add(pos == null ? pg.vInt(0) : pos);
		args.add(tree == null ? pg.emitNull("tree") : tree);
		args.add(prevLog == null ? pg.emitNull("treeLog") : prevLog);
		if (this.useLinkList()) {
			args.add(pg.emitNull("treeLog")); // nextLog;
		}
		return pg.emitFunc("TreeLog", args);
	}

	void loadTreeLog(ParserGenerator<B, C> pg) {
		final int OpNew = 0;
		final int OpTag = 1;
		final int OpValue = 2;
		final int OpLink = 3;
		final boolean UseLinkList = this.useLinkList();
		// final boolean UseLength = !pg.isDefined("alen");
		final boolean UsePointerPosition = pg.usePointerPosition();

		this.defineLib("TreeLog", () -> {
			List<String> fields = new ArrayList<>();
			fields.add("op");
			fields.add("pos");
			fields.add("tree");
			fields.add("prevLog");
			if (UseLinkList) {
				fields.add("nextLog");
			}
			pg.declStruct("TreeLog", fields.toArray(new String[fields.size()]));
		});

		this.defineLib("logT", () -> {
			if (UseLinkList) {
				this.defFunc(pg, this.T("treeLog"), "useTreeLog", "px", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "treeLog", pg.emitGetter("px.treeLog"));
					pg.emitIfStmt(block, pg.emitIsNull(pg.emitGetter("treeLog.nextLog")), false, () -> {
						B block2 = pg.beginBlock();
						pg.Setter(block2, "treeLog.nextLog",
								this.emitNewTreeLog(pg, null, null, null, pg.emitGetter("px.treeLog")));
						return pg.endBlock(block2);
					});
					pg.emitStmt(block, pg.emitReturn(pg.emitGetter("treeLog.nextLog")));
					return pg.endBlock(block);
				});
				this.defFunc(pg, this.T("matched"), "logT", "px", "op", "pos", "tree", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "treeLog", pg.emitFunc("useTreeLog", pg.V("px")));
					pg.Setter(block, "treeLog.op", pg.V("op"));
					pg.Setter(block, "treeLog.pos", pg.V("pos"));
					pg.Setter(block, "treeLog.tree", pg.V("tree"));
					pg.Setter(block, "px.treeLog", pg.V("treeLog"));
					pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
					return (pg.endBlock(block));
				});
			} else {
				this.defFunc(pg, this.T("matched"), "logT", "px", "op", "pos", "tree", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "treeLog", this.emitNewTreeLog(pg, pg.V("op"), pg.V("pos"),
							pg.V("tree"), pg.emitGetter("px.treeLog")));
					pg.Setter(block, "px.treeLog", pg.V("treeLog"));
					pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
					return (pg.endBlock(block));
				});

			}
		});

		this.defineLib("beginT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, this.T("matched"), "beginT", "px", "shift", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpNew),
						pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")), pg.emitNull("tree")));
			});

		});
		this.defineLib("tagT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, this.T("matched"), "tagT", "px", "ntag", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpTag), this.topos(pg, "ntag"), pg.emitNull("tree")));
			});

		});
		this.defineLib("valueT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, this.T("matched"), "valueT", "px", "nvalue", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpValue), this.topos(pg, "nvalue"),
						pg.emitNull("tree")));
			});
		});
		this.defineLib("linkT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, this.T("matched"), "linkT", "px", "nlabel", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpLink), this.topos(pg, "nlabel"),
						pg.emitGetter("px.tree")));
			});
		});
		this.defineLib("foldT", () -> {
			this.makeLib("beginT");
			this.makeLib("linkT");
			this.defFunc(pg, this.T("matched"), "foldT", "px", "shift", "nlabel", () -> {
				C log1 = pg.emitFunc("beginT", pg.V("px"), pg.V("shift"));
				C log2 = pg.emitFunc("linkT", pg.V("px"), pg.V("nlabel"));
				return (pg.emitAnd(log1, log2));
			});
		});
		this.defineLib("gettag", () -> {
			this.defFunc(pg, this.s("Symbol"), "gettag", "ntag", () -> {
				return pg.emitCast("tag", pg.emitArrayIndex(pg.Const("SYMBOLs"), pg.V("ntag")));
			});
			this.defFunc(pg, this.s("Symbol"), "getlabel", "nlabel", () -> {
				return pg.emitCast("label", pg.emitArrayIndex(pg.Const("SYMBOLs"), pg.V("nlabel")));
			});
		});
		this.defineLib("getvalue", () -> {
			this.defFunc(pg, this.T("value"), "getvalue", "nvalue", () -> {
				return pg.emitCast("value", pg.emitArrayIndex(pg.Const("VALUEs"), pg.V("nvalue")));
			});
		});
		this.defineLib("getlength", () -> {
			this.defFunc(pg, this.T("length"), "getlength", "nvalue", () -> {
				return pg.emitCast("length", pg.emitArrayIndex(pg.Const("LENGTHs"), pg.V("nvalue")));
			});
		});

		this.defineLib("endT", () -> {
			this.makeLib("gettag");
			this.makeLib("getvalue");
			this.makeLib("getlength");
			this.defFunc(pg, this.T("matched"), "endT", "px", "shift", "ntag", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, true, "cnt", pg.vInt(0));
				pg.emitVarDecl(block, true, "treeLog", pg.emitGetter("px.treeLog"));
				pg.emitVarDecl(block, true, "prevLog", pg.emitGetter("px.treeLog"));
				pg.emitVarDecl(block, true, "nvalue", pg.vInt(0));
				/* while */
				C loopCond = pg.emitOp(pg.emitGetter("prevLog.op"), "!=", pg.vInt(OpNew));
				C loopNext = pg.emitAssign("prevLog", pg.emitGetter("prevLog.prevLog"));
				C ifLinkCond = pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpLink));

				pg.emitWhileStmt(block, loopCond, () -> {
					C ifTagCond = pg.emitAnd(pg.emitOp(pg.V("ntag"), "==", pg.vInt(0)),
							pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpTag)));
					C ifValueCond = pg.emitAnd(pg.emitOp(pg.V("nvalue"), "==", pg.vInt(0)),
							pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpValue)));
					B block2 = pg.beginBlock();
					pg.emitIfStmt(block2, ifLinkCond, false, () -> {
						return pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1)));
					});
					pg.emitIfStmt(block2, ifTagCond, false, () -> {
						return pg.emitAssign("ntag", this.frompos(pg, pg.emitGetter("prevLog.pos")));
					});
					pg.emitIfStmt(block2, ifValueCond, false, () -> {
						B block3 = pg.beginBlock();
						pg.Assign(block3, "nvalue", this.frompos(pg, pg.emitGetter("prevLog.pos")));
						return pg.endBlock(block3);
					});
					pg.emitStmt(block2, loopNext);
					return pg.endBlock(block2);
				});
				// pg.emitAssign("tag", ,
				// pg.emitGetter("prevLog.pos")));
				// pg.Assign(block3, "value", pg.emitFunc("getvalue",
				// pg.emitGetter("prevLog.pos")));
				// if (UseLength) {
				// pg.Assign(block3, "length", pg.emitFunc("getlength",
				// pg.emitGetter("prevLog.pos")));
				// }

				List<C> param = new ArrayList<>();
				param.add(pg.emitFunc("gettag", pg.V("ntag")));
				param.add(pg.emitGetter("px.inputs"));
				param.add(pg.emitGetter("prevLog.pos"));
				param.add(pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")));
				param.add(pg.V("cnt"));
				List<C> param2 = new ArrayList<>();
				param2.add(pg.emitFunc("gettag", pg.V("ntag")));
				param2.add(pg.emitFunc("getvalue", pg.V("nvalue")));
				param2.add(pg.vInt(0));
				if (UsePointerPosition) {
					param2.add(pg.emitOp(pg.emitFunc("getvalue", pg.V("nvalue")), "+",
							pg.emitFunc("getlength", pg.V("nvalue"))));
				} else {
					param2.add(pg.emitFunc("getlength", pg.V("nvalue")));
				}
				param2.add(pg.V("cnt"));
				C newTree = pg.emitIf(pg.emitOp(pg.V("nvalue"), "==", pg.vInt(0)),
						pg.emitApply(pg.emitGetter("px.newFunc"), param),
						pg.emitApply(pg.emitGetter("px.newFunc"), param2));
				pg.Setter(block, "px.tree", newTree);
				// set..
				pg.emitStmt(block, pg.emitAssign("prevLog", pg.V("treeLog")));

				pg.emitWhileStmt(block, loopCond, () -> {
					B block2 = pg.beginBlock();
					pg.emitIfStmt(block2, ifLinkCond, false, () -> {
						B block3 = pg.beginBlock();
						pg.emitStmt(block3, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "-", pg.vInt(1))));
						C setFunc = pg.emitApply(pg.emitGetter("px.setFunc"), pg.emitGetter("px.tree"), pg.V("cnt"),
								pg.emitFunc("getlabel", this.frompos(pg, pg.emitGetter("prevLog.pos"))),
								pg.emitGetter("prevLog.tree"));
						pg.emitStmt(block3, pg.emitSetter("px.tree", setFunc));
						return pg.endBlock(block3);
					});
					pg.emitStmt(block2, loopNext);
					return pg.endBlock(block2);
				});
				pg.emitStmt(block, pg.emitBack("treeLog", pg.emitGetter("prevLog.prevLog")));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return (pg.endBlock(block));
			});
		});
		this.defineLib("backLink", () -> {
			this.makeLib("linkT");
			this.defFunc(pg, pg.T("matched"), "backLink", "px", "treeLog", "nlabel", "tree", () -> {
				B block = pg.beginBlock();
				pg.emitStmt(block, pg.emitBack("treeLog", pg.V("treeLog")));
				pg.emitStmt(block, pg.emitFunc("linkT", pg.V("px"), pg.V("nlabel")));
				pg.emitStmt(block, pg.emitBack("tree", pg.V("tree")));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return (pg.endBlock(block));
			});
		});
	}

	/* Memo */

	void loadMemo(ParserGenerator<B, C> pg, ParserGrammar g) {
		final int memoSize = g.getMemoPointSize();
		final int window = 64;
		final int ResultFail = 0;
		final int ResultSucc = 1;
		final int ResultUnfound = 2;
		this.defineLib("MemoEntry", () -> {
			pg.declStruct("MemoEntry", "key", "result", "pos", "tree", "state");
		});

		this.defineLib("newMemos", () -> {
			this.defFunc(pg, this.T("memos"), "newMemos", "length", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, true, "memos", pg.emitNewArray(this.T("m"), pg.V("length")));
				pg.emitVarDecl(block, false, "cnt", pg.vInt(0));
				/* while */
				C loopCond = pg.emitOp(pg.V("cnt"), "<", pg.V("length"));
				pg.emitWhileStmt(block, loopCond, () -> {
					B block2 = pg.beginBlock();
					C right = pg.emitFunc("MemoEntry", pg.vInt(-1), pg.vInt(0), pg.vInt(0), pg.emitNull("tree"),
							pg.emitNull("state"));
					if (!this.isDefined("List")) {
						C left = pg.emitArrayIndex(pg.V("memos"), pg.V("cnt"));
						pg.emitStmt(block2, pg.emitAssign2(left, right));
					} else {
						pg.emitStmt(block2, pg.emitFunc("List.add", pg.V("memos"), right));
					}
					pg.emitStmt(block2, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1))));
					return pg.endBlock(block2);
				});
				pg.emitStmt(block, pg.emitReturn(pg.V("memos")));
				return (pg.endBlock(block));
			});
		});

		this.defineLib("longkey", () -> {
			this.defFunc(pg, this.T("key"), "longkey", "key", "memoPoint", () -> {
				C key = pg.emitOp(pg.V("key"), "*", pg.vInt(window));
				key = pg.emitOp(key, "+", pg.V("memoPoint"));
				return (key);
			});
		});
		this.defineLib("getMemo", () -> {
			this.defFunc(pg, pg.T("m"), "getMemo", "px", "key", () -> {
				C index = pg.emitOp(pg.V("key"), "%", pg.vInt(memoSize * window + 1));
				if (this.isDefined("Int64->Int")) {
					index = pg.emitFunc("Int64->Int", index);
				}
				C m = pg.emitArrayIndex(pg.emitGetter("px.memos"), index);
				return (m);
			});
		});
		this.defineLib2("memo", (Object suffix) -> {
			int stacks = (Integer) suffix;
			pg.makeLib("ParserFunc");
			this.defineLib("memo" + suffix, () -> {
				this.makeLib("longkey");
				this.makeLib("getMemo");
				this.defFunc(pg, this.T("result"), "consumeM" + suffix, "px", "m", () -> {
					B block = pg.beginBlock();
					pg.Setter(block, "px.pos", pg.emitGetter("m.pos"));
					if ((stacks & TREE) == TREE) {
						pg.Setter(block, "px.tree", pg.emitGetter("m.tree"));
					}
					pg.emitStmt(block, pg.emitReturn(pg.emitGetter("m.result")));
					return (pg.endBlock(block));
				});
				this.defFunc(pg, this.T("result"), "lookupM" + suffix, "px", "memoPoint", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "key",
							pg.emitFunc("longkey", pg.emitGetter("px.pos"), pg.V("memoPoint")));
					pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
					// m.key == key
					C cond = pg.emitOp(pg.emitGetter("m.key"), "==", pg.V("key"));
					if (pg.isStateful()) {
						// m.key == key && m.state == state
						cond = pg.emitAnd(cond, pg.emitOp(pg.emitGetter("m.state"), "==", pg.emitGetter("px.state")));
					}
					C then = pg.emitFunc("consumeM" + suffix, pg.V("px"), pg.V("m"));
					C result = pg.emitIf(cond, then, pg.vInt(ResultUnfound));
					pg.emitStmt(block, pg.emitReturn(result));
					return (pg.endBlock(block));
				});
				this.makeLib("storeM");
				pg.declFunc(pg.s("Tmatched"), "memo" + suffix, "px", "memoPoint", "f", () -> {
					C lookup = pg.emitFunc("lookupM" + suffix, pg.V("px"), pg.V("memoPoint"));
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "pos", pg.emitGetter("px.pos"));
					ArrayList<C> cases = new ArrayList<>();
					cases.add(pg.emitFail());
					cases.add(pg.emitSucc());
					cases.add(pg.emitFunc("storeM", pg.V("px"), pg.V("memoPoint"), pg.V("pos"),
							pg.emitApply(pg.V("f"), pg.V("px"))));
					pg.emitStmt(block, pg.emitDispatch(lookup, cases));
					return pg.endBlock(block);
				});
			});
		});

		this.defineLib("storeM", () -> {
			this.defFunc(pg, this.T("matched"), "storeM", "px", "memoPoint", "pos", "matched", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "key", pg.emitFunc("longkey", pg.V("pos"), pg.V("memoPoint")));
				pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
				pg.Setter(block, "m.key", pg.V("key"));
				pg.Setter(block, "m.result", pg.emitIf(pg.V("matched"), pg.vInt(ResultSucc), pg.vInt(ResultFail)));
				pg.Setter(block, "m.pos", pg.emitIf(pg.V("matched"), pg.emitGetter("px.pos"), pg.V("pos")));
				pg.Setter(block, "m.tree", pg.emitGetter("px.tree"));
				if (pg.isStateful()) {
					pg.Setter(block, "m.state", pg.emitGetter("px.state"));
				}
				pg.emitStmt(block, pg.emitReturn(pg.V("matched")));
				return (pg.endBlock(block));
			});
		});
	}

	// // State
	//
	// void makeUseStateFunc(ParserGenerator<B, C> pg) {
	// this.defFunc(pg, this.T("state"), "useState", "px", () -> {
	// B block = pg.beginBlock();
	// pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.uState"), "==",
	// pg.emitNull()), false, () -> {
	// return pg.emitReturn(pg.emitFunc(pg.T("state")));
	// });
	// pg.emitVarDecl(block, false, "uState", pg.emitGetter("px.State"));
	// pg.Setter(block, "px.uState", pg.emitGetter("uLog.prevState"));
	// pg.emitStmt(block, pg.emitReturn(pg.V("uState")));
	// return pg.endBlock(block);
	// });
	// }
	//
	// void makeUnuseStateFunc(ParserGenerator<B, C> pg) {
	// this.defFunc(pg, this.T("state"), pg.s("Ustate"), "px", "state", () -> {
	// B block = pg.beginBlock();
	// pg.emitVarDecl(block, false, "uState", pg.emitGetter("px.treeState"));
	// pg.emitWhileStmt(block, pg.emitOp(pg.V("uState"), "!=",
	// pg.V("treeState")), () -> {
	// B block2 = pg.beginBlock();
	// pg.emitVarDecl(block2, false, "prevState", pg.V("uState"));
	// pg.emitStmt(block2, pg.emitAssign("uState",
	// pg.emitGetter("uState.prevState")));
	// pg.emitStmt(block2, pg.emitSetter("prevState.prevState",
	// pg.emitGetter("px.uState")));
	// pg.emitStmt(block2, pg.emitSetter("px.uState", pg.V("prevState")));
	// return pg.endBlock(block2);
	// });
	// pg.emitStmt(block, pg.emitReturn(pg.V("state")));
	// return pg.endBlock(block);
	// });
	// }

	void loadState(ParserGenerator<B, C> pg) {

		this.defineLib("State", () -> {
			pg.declStruct("State", "ntag", "cnt", "value", "prevState");
		});

		// this.defineLib("createS", () -> {
		// this.defFunc(pg, this.T("state"), "createS", "ntag", "cnt", "value",
		// "prevState", () -> {
		// B block = pg.beginBlock();
		// C newFunc = /*
		// * pg.isDefined("Ustate") ? pg.emitFunc("useState",
		// * pg.V("px")) :
		// */ pg.emitFunc("State");
		// pg.emitVarDecl(block, false, "state", newFunc);
		// pg.Setter(block, "state.tag", pg.V("tag"));
		// pg.Setter(block, "state.cnt", pg.V("cnt"));
		// pg.Setter(block, "state.value", pg.V("value"));
		// pg.Setter(block, "state.prevState", pg.V("prevState"));
		// pg.emitStmt(block, pg.emitReturn(pg.V("state")));
		// return (pg.endBlock(block));
		// });
		// });

		this.defineLib("symbolS", () -> {
			this.makeLib("extract");
			this.defFunc(pg, this.T("matched"), "symbolS", "px", "state", "ntag", "pos", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "value", pg.emitFunc("extract", pg.V("px"), pg.V("pos")));
				pg.emitVarDecl(block, false, "length", pg.emitOp(pg.emitGetter("px.pos"), "-", pg.V("pos")));
				pg.emitStmt(block, pg.emitSetter("px.state", //
						pg.emitFunc("State", pg.V("ntag"), pg.V("length"), pg.V("value"), pg.emitGetter("px.state"))));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return pg.endBlock(block);
			});
		});

		this.defineLib("removeS", () -> {
			this.defFunc(pg, this.T("state"), "removeS_", "ntag", "state", () -> {
				C cond0 = pg.emitOp(pg.emitGetter("state.ntag"), "==", pg.V("ntag"));
				C then0 = pg.emitFunc("removeS_", pg.V("ntag"), pg.emitGetter("state.prevState"));
				C else0 = pg.emitFunc("State", pg.emitGetter("state.ntag"), pg.emitGetter("state.cnt"),
						pg.emitGetter("state.value"), pg.emitGetter("state.prevState"));
				return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitNull("state"), pg.emitIf(cond0, then0, else0));
			});
			this.defFunc(pg, this.T("matched"), "removeS", "px", "state", "ntag", "pos", () -> {
				B block = pg.beginBlock();
				pg.emitStmt(block,
						pg.emitSetter("px.state", pg.emitFunc("removeState", pg.V("ntag"), pg.emitGetter("px.state"))));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return pg.endBlock(block);
			});
		});

		this.defineLib("existsS", () -> {
			this.defFunc(pg, this.T("matched"), "existsS", "px", "state", "ntag", "pos", () -> {
				C cond0 = pg.emitOp(pg.emitGetter("state.ntag"), "==", pg.V("ntag"));
				C then0 = pg.emitSucc();
				C else0 = pg.emitFunc("existsS", pg.V("px"), pg.emitGetter("state.prevState"), pg.V("ntag"),
						pg.V("pos"));
				return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitFail(), pg.emitIf(cond0, then0, else0));
			});
		});
		this.defineLib2("existsS", (Object thunk) -> {
			String f = "existsS" + thunk;
			byte[] b = thunk.toString().getBytes();
			this.defineLib(f, () -> {
				this.defFunc(pg, this.T("matched"), f, "px", "state", "ntag", "pos", () -> {
					C cond0 = pg.emitFunc("memcmp", pg.emitGetter("state.value"), pg.vValue(thunk.toString()),
							pg.vInt(b.length));
					C then0 = pg.emitSucc();
					C else0 = pg.emitFunc(f, pg.V("px"), pg.emitGetter("state.prevState"), pg.V("ntag"), pg.V("pos"));
					C cond1 = pg.emitOp(pg.emitGetter("state.ntag"), "==", pg.V("ntag"));
					C then1 = pg.emitIf(cond0, then0, else0);
					return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitFail(), pg.emitIf(cond1, then1, else0));
				});
			});
		});
		this.defineLib("matchS", () -> {
			this.makeLib("nextN");
			this.defFunc(pg, this.T("matched"), "matchS", "px", "state", "ntag", "pos", () -> {
				C cond0 = pg.emitOp(pg.emitGetter("state.ntag"), "==", pg.V("ntag"));
				C then0 = pg.emitFunc("nextN", pg.V("px"), pg.emitGetter("state.value"), pg.emitGetter("state.cnt"));
				C else0 = pg.emitFunc("matchS", pg.V("px"), pg.emitGetter("state.prevState"), pg.V("ntag"),
						pg.V("pos"));
				return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitFail(), pg.emitIf(cond0, then0, else0));
			});
		});
	}

	// Tree

	void loadMain(ParserGenerator<B, C> pg) {
		this.defineLib("newSampleTree", () -> {
			this.defFunc(pg, this.T("tree"), "newSampleTree", "tag", "inputs", "pos", "epos", "cnt", () -> {
				return pg.emitIf(pg.emitOp(pg.V("cnt"), "==", pg.vInt(0)), //
						pg.emitNewToken(pg.V("tag"), pg.V("inputs"), pg.V("pos"), pg.V("epos")), //
						pg.emitNewTree(pg.V("tag"), pg.V("cnt")));
			});
		});
		this.defineLib("setSampleTree", () -> {
			this.defFunc(pg, this.T("tree"), "setSampleTree", "tree", "cnt", "label", "child", () -> {
				return pg.emitSetTree(pg.V("tree"), pg.V("cnt"), pg.V("label"), pg.V("child"));
			});
		});
		this.defineLib("parse", () -> {
			pg.makeLib("newSampleTree");
			pg.makeLib("setSampleTree");
			pg.makeLib("newMemos");

			this.defFunc(pg, this.T("tree"), "parse", "text", "newFunc", "setFunc", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "inputs", pg.emitFunc("String->Byte[]", pg.emitInputString(pg.V("text"))));
				if (this.isDefined("zero")) {
					pg.emitVarDecl(block, false, "length",
							pg.emitOp(pg.emitArrayLength(pg.V("inputs")), "-", pg.vInt(1)));
				} else {
					pg.emitVarDecl(block, false, "length", pg.emitArrayLength(pg.V("inputs")));
				}
				// New
				ArrayList<C> args = new ArrayList<>();
				args.add(pg.V("inputs"));
				args.add(pg.V("length"));
				args.add(pg.vInt(0));
				args.add(pg.vInt(0));
				args.add(pg.emitNull("tree"));
				args.add(this.emitNewTreeLog(pg, null, null, null, null));
				args.add(pg.IfNull(pg.V("newFunc"), pg.emitFuncRef("newSampleTree")));
				if (!pg.isFunctional()) {
					this.makeLib("TreeSetFunc");
					args.add(pg.IfNull(pg.V("setFunc"), pg.emitFuncRef("setSampleTree")));
				}
				// if (pg.isStateful()) {
				args.add(pg.emitNull("state"));
				// }
				// if (g.getMemoPointSize() > 0) {
				args.add(pg.emitFunc("newMemos", pg.vInt(pg.grammar.getMemoPointSize() * 64 + 1)));
				// }
				pg.emitVarDecl(block, false, "px", pg.emitFunc("NezParserContext", args));
				pg.emitStmt(block, pg.emitReturn(
						pg.emitIf(pg.emitNonTerminal("e0"), pg.emitGetter("px.tree"), pg.emitNull("tree"))));
				return (pg.endBlock(block));
			});
		});

		this.defineLib("*", () -> {

		});
	}

}
