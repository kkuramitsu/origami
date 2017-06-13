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
 **********************************************************************/

package blue.origami.nezcc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

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

	public final static int Aprivate = 0;
	public final static int Apublic = 1;
	public final static int Arec = 1 << 1;
	public final static int Apeg = 1 << 2;
	public final static int Anoparam = 1 << 2;

	protected void defFunc(ParserGenerator<B, C> pg, int acc, String ret, String funcName, String[] params,
			Supplier<C> block) {
		pg.declFunc(acc, ret, funcName, params, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, int acc, String ret, String funcName, String a0,
			Supplier<C> block) {
		if (a0.indexOf(",") > 0) {
			this.defFunc(pg, acc, ret, funcName, a0.split(","), block);
		} else {
			this.defFunc(pg, acc, ret, funcName, new String[] { a0 }, block);
		}
	}

	protected void defFunc(ParserGenerator<B, C> pg, int acc, String ret, String funcName, String a0, String a1,
			Supplier<C> block) {
		this.defFunc(pg, acc, ret, funcName, new String[] { a0, a1 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, int acc, String ret, String funcName, String a0, String a1,
			String a2, Supplier<C> block) {
		this.defFunc(pg, acc, ret, funcName, new String[] { a0, a1, a2 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, int acc, String ret, String funcName, String a0, String a1,
			String a2, String a3, Supplier<C> block) {
		this.defFunc(pg, acc, ret, funcName, new String[] { a0, a1, a2, a3 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, int acc, String ret, String funcName, String a0, String a1,
			String a2, String a3, String a4, Supplier<C> block) {
		this.defFunc(pg, acc, ret, funcName, new String[] { a0, a1, a2, a3, a4 }, block);
	}

	void loadContext(ParserGenerator<B, C> pg, ParserGrammar g) {
		if (this.isDefined("TList")) {
			this.defineLib("TreeFunc", () -> {
				pg.declFuncType(this.T("tree"), "TreeFunc", "tag", "inputs", "spos", "epos", "subTrees");
			});
		} else {
			this.defineLib("TreeFunc", () -> {
				pg.declFuncType(this.T("tree"), "TreeFunc", "tag", "inputs", "spos", "epos", "n");
			});
			this.defineLib("TreeSetFunc", () -> {
				pg.declFuncType(this.T("tree"), "TreeSetFunc", "tree", "n", "label", "child");
			});
		}
		this.defineLib("ParserFunc", () -> {
			pg.declFuncType(this.T("matched"), "ParserFunc", "px");
		});
		this.defineLib("getbyte", () -> {
			this.makeLib("movep");
			this.defFunc(pg, 0, this.T("c"), "getbyte", "px", () -> {
				C expr = pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos"));
				return (pg.emitUnsigned(expr));
			});
		});
		this.defineLib("nextbyte", () -> {
			this.defFunc(pg, 0, this.T("c"), "nextbyte", "px", () -> {
				if (pg.isDefined("++")) {
					C inc = pg.emitFunc("++", pg.emitGetter("px.pos"));
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
		this.defineLib("movep", () -> {
			this.defFunc(pg, 0, this.T("matched"), "movep", "px", "shift", () -> {
				B block = pg.beginBlock();
				pg.Setter(block, "px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return (pg.endBlock(block));
			});
		});
		this.defineLib("neof", () -> {
			this.defFunc(pg, 0, this.T("matched"), "neof", "px", () -> {
				C length = pg.emitGetter("px.length");
				return (pg.emitOp(pg.emitGetter("px.pos"), "<", length));
			});
		});

		/* backpos(px, pos) */
		this.defineLib("backpos", () -> {
			this.defFunc(pg, 0, this.T("pos"), "backpos", "px", "pos", () -> {
				B block = pg.beginBlock();
				pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.headpos"), "<", pg.V("pos")), false, () -> {
					return pg.emitSetter("px.headpos", pg.V("pos"));
				});
				pg.Return(block, pg.V("pos"));
				return pg.endBlock(block);
			});
		});

		this.defineLib("bits32", () -> {
			this.defFunc(pg, 0, this.T("matched"), "bits32", "bits", "n", () -> {
				// (bits[n / 32] & (1 << (n % 32))) != 0
				C index = pg.emitOp(pg.V("n"), "/", pg.vInt(32));
				C mask = pg.emitOp(pg.vInt(1), "<<", pg.emitGroup(pg.emitOp(pg.V("n"), "%", pg.vInt(32))));
				C expr = pg.emitArrayIndex(pg.V("bits"), index);
				expr = pg.emitOp(expr, "&", pg.emitGroup(mask));
				expr = pg.emitOp(pg.emitGroup(expr), "!=", pg.vInt(0));
				return (expr);
			});
		});
		this.defineLib("next1", () -> {
			this.makeLib("nextbyte");
			this.defFunc(pg, 0, this.T("matched"), "next1", "px", "c", () -> {
				C expr = pg.emitFunc("nextbyte", pg.V("px"));
				expr = pg.emitOp(expr, "==", pg.V("c"));
				return (expr);
			});
		});

		this.defineLib("nextN", () -> {
			C pos_plen = pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("length"));
			C cnt_len = pg.emitOp(pg.V("cnt"), "<", pg.V("length"));
			C cnt_pp = pg.emitOp(pg.V("cnt"), "+", pg.vInt(1));
			C pos_pcnt = pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("cnt"));
			C input = pg.emitArrayIndex(pg.emitGetter("px.inputs"), pos_pcnt);
			C value = pg.emitArrayIndex(pg.V("value"), pg.V("cnt"));
			C cmpr = pg.emitOp(input, "==", value);
			C cond = pg.emitAnd(cnt_len, cmpr);
			if (this.isDefined("while")) {
				this.defFunc(pg, 0, pg.T("matched"), "nextN", "px", "value", "length", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, true, "cnt", pg.vInt(0));
					pg.emitIfStmt(block, pg.emitOp(pos_plen, "<=", pg.emitGetter("px.length")), false, () -> {
						B block2 = pg.beginBlock();
						pg.emitWhileStmt(block2, cond, () -> {
							B block3 = pg.beginBlock();
							pg.Assign(block3, "cnt", cnt_pp);
							return pg.endBlock(block3);
						});
						pg.Setter(block2, "px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("cnt")));
						return pg.endBlock(block2);
					});
					pg.Return(block, pg.emitOp(pg.V("cnt"), "==", pg.V("length")));
					return pg.endBlock(block);
				});
			} else {
				this.makeLib("next1");
				this.defFunc(pg, 0, pg.T("matched"), "rnextN", "px", "value", "cnt", "length", () -> {
					C cond2 = pg.emitFunc("next1", pg.V("px"), pg.emitUnsigned(value));
					C rec = pg.emitFunc("rnextN", pg.V("px"), pg.V("value"), cnt_pp, pg.V("length"));
					return pg.emitIf(cnt_len, pg.emitAnd(cond2, rec), pg.emitSucc());
				});
				this.defFunc(pg, 0, pg.T("matched"), "nextN", "px", "value", "length", () -> {
					return pg.emitFunc("rnextN", pg.V("px"), pg.V("value"), pg.vInt(0), pg.V("length"));
				});
			}
		});

		this.defineLib("fTrue", () -> {
			this.defFunc(pg, 0, pg.T("matched"), "fTrue", "px", () -> {
				return pg.emitSucc();
			});
		});
		this.defineLib("fFalse", () -> {
			this.defFunc(pg, 0, pg.T("matched"), "fFalse", "px", () -> {
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
					this.defFunc(pg, 0, pg.T("matched"), fname, "px", pname, () -> {
						B block = pg.beginBlock();
						pg.emitWhileStmt(block, inner, () -> {
							return pg.emitMove(pg.vInt(1));
						});
						pg.Return(block, pg.emitSucc());
						return pg.endBlock(block);
					});
				});
			}
		});

		this.defineLib2("option", (Object thunk) -> {

		});

		this.defineLib("NezParserContext", () -> {
			List<String> fields = new ArrayList<>();
			fields.add("inputs");
			fields.add("length");
			fields.add("pos");
			fields.add("headpos");
			fields.add("tree");
			this.makeLib("TreeLog");
			fields.add("treeLog");
			this.makeLib("TreeFunc");
			fields.add("newFunc");
			if (!pg.isDefined("TList")) {
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
			this.defFunc(pg, 0, pg.T("matched"), fname, "px", pname, () -> {
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

	// private C frompos(ParserGenerator<B, C> pg, C expr) {
	// if (pg.usePointerPosition()) {
	// return pg.emitCast("ntag", expr);
	// }
	// return expr;
	// }

	private C emitNewTreeLog(ParserGenerator<B, C> pg, C op, C pos, C tree, C prevLog) {
		List<C> args = new ArrayList<>();
		args.add(op == null ? pg.vInt(0) : op);
		args.add(pos == null ? pg.vInt(0) : pos);
		args.add(tree == null ? (pg.isDefined("null") ? pg.emitNull("tree") : pg.emitGetter("px.tree")) : tree);
		args.add(prevLog == null ? pg.emitNull("treeLog") : prevLog);
		if (this.useLinkList()) {
			args.add(pg.emitNull("treeLog")); // nextLog;
		}
		return pg.emitNew("TreeLog", args);
	}

	private C ApplyTreeFunc(ParserGenerator<B, C> pg, C spos, C epos, C sub) {
		List<C> param = new ArrayList<>();
		param.add(pg.emitFunc("gettag", pg.V("ntag")));
		param.add(pg.emitGetter("px.inputs"));
		param.add(spos);
		param.add(epos);
		param.add(sub);
		List<C> param2 = new ArrayList<>();
		param2.add(pg.emitFunc("gettag", pg.V("ntag")));
		param2.add(pg.emitFunc("getvalue", pg.V("nvalue")));
		param2.add(pg.vInt(0));
		param2.add(pg.emitFunc("getlength", pg.V("nvalue")));
		param2.add(sub);
		return pg.emitIf(pg.emitOp(pg.V("nvalue"), "==", pg.vInt(0)), pg.emitApply(pg.emitGetter("px.newFunc"), param),
				pg.emitApply(pg.emitGetter("px.newFunc"), param2));
	}

	void loadTreeLog(ParserGenerator<B, C> pg) {
		final int OpNew = 0;
		final int OpTag = 1;
		final int OpValue = 2;
		final int OpLink = 3;
		final boolean UseLinkList = this.useLinkList();
		// final boolean UseLength = !pg.isDefined("alen");
		final boolean UsePointerPosition = false; // Objects.equals(this.T("pos"),
													// this.T("inputs"));
		final boolean Optional = pg.isDefined("Option");

		this.defineLib("TreeLog", () -> {
			List<String> fields = new ArrayList<>();
			fields.add("op");
			fields.add("log");
			fields.add("tree");
			fields.add("prevLog");
			if (UseLinkList) {
				fields.add("nextLog");
			}
			pg.declStruct("TreeLog", fields.toArray(new String[fields.size()]));
		});

		this.defineLib("logT", () -> {
			if (UseLinkList) {
				this.defFunc(pg, 0, this.T("treeLog"), "useTreeLog", "px", () -> {
					B block = pg.beginBlock();
					if (Optional) {
						pg.emitVarDecl(block, false, "tcur", pg.emitFunc("Option.get", pg.emitGetter("px.treeLog")));
					} else {
						pg.emitVarDecl(block, false, "tcur", pg.emitGetter("px.treeLog"));
					}
					pg.emitIfStmt(block, pg.emitIsNull(pg.emitGetter("tcur.nextLog")), false, () -> {
						B block2 = pg.beginBlock();
						pg.Setter(block2, "tcur.nextLog",
								this.emitNewTreeLog(pg, null, null, null, pg.emitGetter("px.treeLog")));
						return pg.endBlock(block2);
					});
					pg.emitStmt(block, pg.emitReturn(pg.emitGetter("tcur.nextLog")));
					return pg.endBlock(block);
				});

				this.defFunc(pg, 0, this.T("matched"), "logT", "px", "op", "log", "tree", () -> {
					B block = pg.beginBlock();
					if (Optional) {
						pg.emitVarDecl(block, false, "treeLog", pg.emitFunc("useTreeLog", pg.V("px")));
						pg.emitVarDecl(block, false, "tcur", pg.emitFunc("Option.get", pg.V("treeLog")));
					} else {
						pg.emitVarDecl(block, false, "tcur", pg.emitFunc("useTreeLog", pg.V("px")));
					}
					pg.Setter(block, "tcur.op", pg.V("op"));
					pg.Setter(block, "tcur.log", pg.V("log"));
					pg.Setter(block, "tcur.tree", pg.V("tree"));
					if (Optional) {
						pg.Setter(block, "px.treeLog", pg.V("treeLog"));
					} else {
						pg.Setter(block, "px.treeLog", pg.V("tcur"));
					}
					pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
					return (pg.endBlock(block));
				});
			} else {
				this.defFunc(pg, 0, this.T("matched"), "logT", "px", "op", "log", "tree", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "treeLog", this.emitNewTreeLog(pg, pg.V("op"), pg.V("log"),
							pg.V("tree"), pg.emitGetter("px.treeLog")));
					pg.Setter(block, "px.treeLog", pg.V("treeLog"));
					pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
					return (pg.endBlock(block));
				});

			}
		});

		final C nullTree = this.isDefined("null") ? pg.emitNull("tree") : pg.emitGetter("px.tree");
		final C pos_ = pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift"));
		final C pos = (UsePointerPosition) ? pg.emitOp(pg.emitGroup(pos_), "-", pg.emitGetter("px.inputs")) : pos_;

		this.defineLib("beginT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, 0, this.T("matched"), "beginT", "px", "shift", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpNew), pg.emitGroup(pos), nullTree));
			});

		});
		this.defineLib("tagT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, 0, this.T("matched"), "tagT", "px", "ntag", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpTag), pg.V("ntag"), nullTree));
			});

		});
		this.defineLib("valueT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, 0, this.T("matched"), "valueT", "px", "nvalue", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpValue), pg.V("nvalue"), nullTree));
			});
		});
		this.defineLib("linkT", () -> {
			this.makeLib("logT");
			this.defFunc(pg, 0, this.T("matched"), "linkT", "px", "nlabel", "tree", () -> {
				return (pg.emitFunc("logT", pg.V("px"), pg.vInt(OpLink), pg.V("nlabel"), pg.V("tree")));
			});
		});
		this.defineLib("backLink", () -> {
			this.makeLib("linkT");
			this.defFunc(pg, 0, pg.T("matched"), "backLink", "px", "treeLog", "nlabel", "tree", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "tree0", pg.emitGetter("px.tree"));
				// pg.emitStmt(block, pg.emitBack("treeLog", pg.V("treeLog")));
				// pg.emitStmt(block, pg.emitBack("tree", pg.V("tree")));
				pg.emitBack2(block, "treeLog", "tree");
				pg.emitStmt(block, pg.emitReturn(pg.emitFunc("linkT", pg.V("px"), pg.V("nlabel"), pg.V("tree0"))));
				return (pg.endBlock(block));
			});
		});

		this.defineLib("foldT", () -> {
			this.makeLib("beginT");
			this.makeLib("linkT");
			this.defFunc(pg, 0, this.T("matched"), "foldT", "px", "shift", "nlabel", () -> {
				C log1 = pg.emitFunc("beginT", pg.V("px"), pg.V("shift"));
				C log2 = pg.emitFunc("linkT", pg.V("px"), pg.V("nlabel"), pg.emitGetter("px.tree"));
				return (pg.emitAnd(log1, log2));
			});
		});
		this.defineLib("gettag", () -> {
			this.defFunc(pg, 0, this.s("Symbol"), "gettag", "ntag", () -> {
				return pg.emitConv("tag", pg.emitArrayIndex(pg.Const("nezsymbols"), pg.V("ntag")));
			});
			this.defFunc(pg, 0, this.s("Symbol"), "getlabel", "nlabel", () -> {
				return pg.emitConv("label", pg.emitArrayIndex(pg.Const("nezsymbols"), pg.V("nlabel")));
			});
		});
		this.defineLib("getvalue", () -> {
			this.defFunc(pg, 0, this.T("value"), "getvalue", "nvalue", () -> {
				return pg.emitConv("value", pg.emitArrayIndex(pg.Const("nezvalues"), pg.V("nvalue")));
			});
		});
		this.defineLib("getlength", () -> {
			this.defFunc(pg, 0, this.T("length"), "getlength", "nvalue", () -> {
				return pg.emitConv("length", pg.emitArrayIndex(pg.Const("nezvaluesizes"), pg.V("nvalue")));
			});
		});

		C NotNew = pg.emitOp(pg.emitGetter("tcur.op"), "!=", pg.vInt(OpNew));
		C ifLink = pg.emitOp(pg.emitGetter("tcur.op"), "==", pg.vInt(OpLink));
		C ifTag = pg.emitAnd(pg.emitOp(pg.V("ntag"), "==", pg.vInt(0)),
				pg.emitOp(pg.emitGetter("tcur.op"), "==", pg.vInt(OpTag)));
		C ifValue = pg.emitAnd(pg.emitOp(pg.V("nvalue"), "==", pg.vInt(0)),
				pg.emitOp(pg.emitGetter("tcur.op"), "==", pg.vInt(OpValue)));
		C label = pg.emitFunc("getlabel", pg.emitGetter("tcur.log"));
		C subTrees = pg.emitFunc("TList.cons", label, pg.emitGetter("tcur.tree"), pg.V("subTrees"));
		C prevLog = Optional ? pg.emitFunc("Option.get", pg.emitGetter("tcur.prevLog")) : pg.emitGetter("tcur.prevLog");

		C start = (Optional) ? pg.emitFunc("Option.get", pg.emitGetter("px.treeLog")) : pg.emitGetter("px.treeLog");
		C epos = pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift"));

		this.defineLib("endT", () -> {
			this.makeLib("gettag");
			this.makeLib("getvalue");
			this.makeLib("getlength");

			if (this.isDefined("while")) {
				this.defFunc(pg, 0, this.T("matched"), "endT", "px", "shift", "ntag0", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "epos", pos);
					pg.emitVarDecl(block, true, "tcur", start);
					pg.emitVarDecl(block, true, "ntag", pg.V("ntag0"));
					pg.emitVarDecl(block, true, "nvalue", pg.vInt(0));
					if (this.isDefined("TList")) {
						pg.emitVarDecl(block, true, "subTrees", pg.emitFunc("TList.empty"));
					} else {
						pg.emitVarDecl(block, true, "cnt", pg.vInt(0));
					}
					/* while */
					pg.emitWhileStmt(block, NotNew, () -> {
						B block2 = pg.beginBlock();
						pg.emitIfStmt(block2, ifLink, false, () -> {
							if (this.isDefined("TList")) {
								return pg.emitAssign("subTrees", subTrees);
							} else {
								return pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1)));
							}
						});
						pg.emitIfStmt(block2, ifTag, true, () -> {
							return pg.emitAssign("ntag", pg.emitGetter("tcur.log"));
						});
						pg.emitIfStmt(block2, ifValue, true, () -> {
							return pg.emitAssign("nvalue", pg.emitGetter("tcur.log"));
						});
						pg.Assign(block2, "tcur", prevLog);
						return pg.endBlock(block2);
					});
					if (this.isDefined("TList")) {
						pg.Setter(block, "px.tree",
								this.ApplyTreeFunc(pg, pg.emitGetter("tcur.log"), pg.V("epos"), pg.V("subTrees")));
					} else {
						pg.Setter(block, "px.tree",
								this.ApplyTreeFunc(pg, pg.emitGetter("tcur.log"), pg.V("epos"), pg.V("cnt")));
						pg.Assign(block, "tcur", start);
						pg.emitWhileStmt(block, NotNew, () -> {
							B block2 = pg.beginBlock();
							pg.emitIfStmt(block2, ifLink, false, () -> {
								B block3 = pg.beginBlock();
								pg.emitStmt(block3, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "-", pg.vInt(1))));
								C setFunc = pg.emitApply(pg.emitGetter("px.setFunc"), pg.emitGetter("px.tree"),
										pg.V("cnt"), pg.emitFunc("getlabel", pg.emitGetter("tcur.log")),
										pg.emitGetter("tcur.tree"));
								pg.emitStmt(block3, pg.emitSetter("px.tree", setFunc));
								return pg.endBlock(block3);
							});
							pg.Assign(block2, "tcur", prevLog);
							return pg.endBlock(block2);
						});

					}
					pg.emitStmt(block, pg.emitBack("treeLog", pg.emitGetter("tcur.prevLog")));
					pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
					return (pg.endBlock(block));
				});
			} else {
				C ifLinkNew = pg.emitOp(pg.emitGetter("tcur.op"), "==", pg.vInt(OpNew));
				String[] params = { "px", "tcur", "ntag", "nvalue", "epos", "subTrees" };
				this.defFunc(pg, 0, pg.s("T"), "recT", params, () -> {
					C rec = pg.emitFunc("recT", pg.V("px"), prevLog,
							pg.emitIf(ifTag, pg.emitGetter("tcur.log"), pg.V("ntag")), //
							pg.emitIf(ifValue, pg.emitGetter("tcur.log"), pg.V("nvalue")), pg.V("epos"), //
							pg.emitIf(ifLink, subTrees, pg.V("subTrees")));
					//
					return pg.emitIf(ifLinkNew,
							this.ApplyTreeFunc(pg, pg.emitGetter("tcur.log"), pg.V("epos"), pg.V("subTrees")), rec);
				});

				this.defFunc(pg, 0, pg.T("treeLog"), "rLog", "tcur", () -> {
					return pg.emitIf(ifLinkNew, pg.emitGetter("tcur.prevLog"), pg.emitFunc("rLog", prevLog));
				});

				this.defFunc(pg, 0, this.T("matched"), "endT", "px", "shift", "ntag", () -> {
					B block = pg.beginBlock();
					C rec = pg.emitFunc("recT", pg.V("px"), start, pg.V("ntag"), pg.vInt(0), epos,
							pg.emitFunc("TList.empty"));
					// pg.Setter(block, "px.tree", rec);
					// pg.emitStmt(block, pg.emitBack("treeLog",
					// pg.emitFunc("rLog", start)));
					pg.Setter2(block, "px", "tree", rec, "treeLog", pg.emitFunc("rLog", start));
					pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
					return (pg.endBlock(block));
				});
			}
		});

	}

	/* Memo */

	// @SuppressWarnings("unchecked")
	void loadMemo(ParserGenerator<B, C> pg, ParserGrammar g) {
		final int memoSize = g.getMemoPointSize();
		final int window = 64;
		final int ResultFail = 0;
		final int ResultSucc = 1;
		final int ResultUnfound = 2;

		this.defineLib("MemoEntry", () -> {
			if (this.isDefined("Int64")) {
				pg.declStruct("MemoEntry", "key", "result", "pos", "tree", "state");
			} else {
				pg.declStruct("MemoEntry", "key", "memoPoint", "result", "pos", "tree", "state");
			}
		});

		this.defineLib("newMemos", () -> {
			List<C> l = new ArrayList<>();
			if (this.isDefined("Int64")) {
				l.add(pg.vInt(-1));
			} else {
				l.add(pg.vInt(-1));
				l.add(pg.vInt(-1));
			}
			l.add(pg.vInt(0));
			l.add(pg.vInt(0));
			l.add(pg.V("tree"));
			l.add(pg.emitNull("state"));
			final C newMemo = pg.emitNew("MemoEntry", l);
			if (pg.isDefined("while")) {
				this.defFunc(pg, 0, this.T("memos"), "newMemos", "tree", "length", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, true, "memos", pg.emitNewArray(this.T("m"), pg.V("length")));
					pg.emitVarDecl(block, true, "cnt", pg.vInt(0));
					C loopCond = pg.emitOp(pg.V("cnt"), "<", pg.V("length"));
					pg.emitWhileStmt(block, loopCond, () -> {
						B block2 = pg.beginBlock();
						if (!this.isDefined("ArrayList")) {
							C left = pg.emitArrayIndex(pg.V("memos"), pg.V("cnt"));
							pg.emitStmt(block2, pg.emitAssign2(left, newMemo));
						} else {
							pg.emitStmt(block2, pg.emitFunc("ArrayList.add", pg.V("memos"), newMemo));
						}
						pg.emitStmt(block2, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1))));
						return pg.endBlock(block2);
					});
					pg.emitStmt(block, pg.emitReturn(pg.V("memos")));
					return (pg.endBlock(block));
				});
			} else {
				this.defFunc(pg, 0, this.T("memos"), "rMemo", "memos", "tree", "cnt", "length", () -> {
					C cond = pg.emitOp(pg.V("cnt"), "<", pg.V("length"));
					C expr1 = (this.isDefined("ArrayList")) ? pg.emitFunc("ArrayList.add", pg.V("memos"), newMemo)
							: pg.emitAssign2(pg.emitArrayIndex(pg.V("memos"), pg.V("cnt")), newMemo);
					C expr2 = pg.emitFunc("rMemo", pg.V("memos"), pg.V("tree"), pg.emitOp(pg.V("cnt"), "+", pg.vInt(1)),
							pg.V("length"));
					return pg.emitIf(cond, pg.emitBlockExpr(this.T("memos"), expr1, expr2), pg.V("memos"));
				});
				this.defFunc(pg, 0, this.T("memos"), "newMemos", "tree", "length", () -> {
					return pg.emitFunc("rMemo", pg.emitNewArray(this.T("m"), pg.V("length")), pg.V("tree"), pg.vInt(0),
							pg.V("length"));
				});
			}
		});

		this.defineLib("longkey", () -> {
			this.defFunc(pg, 0, this.T("key"), "longkey", "key", "memoPoint", () -> {
				C key = pg.emitOp(pg.V("key"), "*", pg.emitConv("Int->Int64", pg.vInt(window)));
				key = pg.emitOp(key, "+", pg.emitConv("Int->Int64", pg.V("memoPoint")));
				return (key);
			});
		});
		this.defineLib("getMemo", () -> {
			this.defFunc(pg, 0, pg.T("m"), "getMemo", "px", "key", () -> {
				C index = pg.emitOp(pg.V("key"), "%", pg.emitConv("Int->Int64", pg.vInt(memoSize * window + 1)));
				index = pg.emitConv("Int64->Int", index);
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
				this.defFunc(pg, 0, this.T("result"), "consumeM" + suffix, "px", "m", () -> {
					B block = pg.beginBlock();
					if ((stacks & TREE) == TREE) {
						pg.Setter2(block, "px", "pos", pg.emitGetter("m.pos"), "tree", pg.emitGetter("m.tree"));
					} else {
						pg.Setter(block, "px.pos", pg.emitGetter("m.pos"));
					}
					pg.emitStmt(block, pg.emitReturn(pg.emitGetter("m.result")));
					return (pg.endBlock(block));
				});
				this.defFunc(pg, 0, this.T("result"), "lookupM" + suffix, "px", "memoPoint", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "key", pg.emitFunc("longkey",
							pg.emitConv("Int->Int64", pg.emitGetter("px.pos")), pg.V("memoPoint")));
					pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
					// m.key == key
					C cond = pg.emitOp(pg.emitGetter("m.key"), "==", pg.V("key"));
					if (!this.isDefined("Int64")) {
						cond = pg.emitOp(pg.emitGetter("m.memoPoint"), "==", pg.V("memoPoint"));
						cond = pg.emitAnd(cond, pg.emitOp(pg.emitGetter("m.key"), "==", pg.emitGetter("px.pos")));
					}
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
			this.defFunc(pg, 0, this.T("matched"), "storeM", "px", "memoPoint", "pos", "matched", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "key",
						pg.emitFunc("longkey", pg.emitConv("Int->Int64", pg.V("pos")), pg.V("memoPoint")));
				pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
				if (this.isDefined("Int64")) {
					pg.Setter(block, "m.key", pg.V("key"));
				} else {
					pg.Setter(block, "m.key", pg.V("pos"));
					pg.Setter(block, "m.memoPoint", pg.V("memoPoint"));
				}
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

	void loadState(ParserGenerator<B, C> pg) {
		final boolean Optional = pg.isDefined("Option");
		final String scur = Optional ? "state" : "scur";

		this.defineLib("State", () -> {
			pg.declStruct("State", "ntag", "cnt", "value", "prevState");
		});

		this.defineLib("extract", () -> {
			this.defFunc(pg, 0, this.T("value"), "extract", "px", "pos", () -> {
				if (this.isDefined("Byte[].slice")) {
					return pg.emitFunc("Byte[].slice", pg.emitGetter("px.inputs"), pg.V("pos"),
							pg.emitGetter("px.pos"));
				}
				return pg.emitFunc("Array.slice", pg.emitGetter("px.inputs"), pg.V("pos"), pg.emitGetter("px.pos"));
			});
		});

		this.defineLib("symbolS", () -> {
			this.makeLib("extract");
			this.defFunc(pg, 0, this.T("matched"), "symbolS", "px", "state", "ntag", "pos", () -> {
				List<C> l = new ArrayList<>();
				l.add(pg.V("ntag"));
				l.add(pg.V("length"));
				l.add(pg.V("value"));
				l.add(pg.emitGetter("px.state"));
				//
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "value", pg.emitFunc("extract", pg.V("px"), pg.V("pos")));
				pg.emitVarDecl(block, false, "length", pg.emitOp(pg.emitGetter("px.pos"), "-", pg.V("pos")));
				pg.emitStmt(block, pg.emitSetter("px.state", pg.emitNew("State", l)));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return pg.endBlock(block);
			});
		});

		this.defineLib("trimS", () -> {
			this.defFunc(pg, Arec, this.T("state"), "trimS", "ntag", "scur", () -> {
				C next = Optional ? pg.emitFunc("Option.get", pg.emitGetter("scur.prevState"))
						: pg.emitGetter("scur.prevState");
				C rec = pg.emitIf(pg.emitIsNull(pg.emitGetter("scur.prevState")), pg.emitNull("state"),
						pg.emitFunc("trimS", pg.V("ntag"), next));
				C cond0 = pg.emitOp(pg.emitGetter("scur.ntag"), "==", pg.V("ntag"));
				C else0 = rec;
				List<C> l = new ArrayList<>();
				l.add(pg.emitGetter("scur.ntag"));
				l.add(pg.emitGetter("scur.cnt"));
				l.add(pg.emitGetter("scur.value"));
				l.add(rec);
				C then0 = pg.emitNew("State", l);
				return pg.emitIf(cond0, else0, then0);
			});
			this.defFunc(pg, Arec, this.T("matched"), "removeS", "px", "state", "ntag", "pos", () -> {
				B block = pg.beginBlock();
				pg.emitStmt(block,
						pg.emitSetter("px.state", pg.emitFunc("trimS", pg.V("ntag"), pg.emitGetter("px.state"))));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return pg.endBlock(block);
			});
		});

		this.defineLib("existsS", () -> {
			this.defFunc(pg, Arec, this.T("matched"), "existsS", "px", scur, "ntag", "pos", () -> {
				C cond0 = pg.emitOp(pg.emitGetter("scur.ntag"), "==", pg.V("ntag"));
				C then0 = pg.emitSucc();
				C else0 = pg.emitFunc("existsS", pg.V("px"), pg.emitGetter("scur.prevState"), pg.V("ntag"),
						pg.V("pos"));
				C inner = pg.emitIf(cond0, then0, else0);
				if (Optional) {
					inner = pg.emitVarDecl(false, "scur", pg.emitFunc("Option.get", pg.V("state")), inner);
				}
				return pg.emitIf(pg.emitIsNull(pg.V(Optional ? "state" : "scur")), pg.emitFail(), inner);
			});
		});
		this.defineLib2("existsS", (Object thunk) -> {
			String f = "existsS" + thunk;
			byte[] b = thunk.toString().getBytes();
			this.defineLib(f, () -> {
				this.defFunc(pg, Arec, this.T("matched"), f, "px", scur, "ntag", "pos", () -> {
					C cond0 = pg.emitFunc("memcmp", pg.emitGetter("scur.value"), pg.vValue(thunk.toString()),
							pg.vInt(b.length));
					C then0 = pg.emitSucc();
					C else0 = pg.emitFunc(f, pg.V("px"), pg.emitGetter("scur.prevState"), pg.V("ntag"), pg.V("pos"));
					C cond1 = pg.emitOp(pg.emitGetter("scur.ntag"), "==", pg.V("ntag"));
					C then1 = pg.emitIf(cond0, then0, else0);
					C inner = pg.emitIf(cond1, then1, else0);
					if (Optional) {
						inner = pg.emitVarDecl(false, "scur", pg.emitFunc("Option.get", pg.V("state")), inner);
					}
					return pg.emitIf(pg.emitIsNull(pg.V(Optional ? "state" : "scur")), pg.emitFail(), inner);
				});
			});
		});
		this.defineLib("matchS", () -> {
			this.makeLib("nextN");
			this.defFunc(pg, Arec, this.T("matched"), "matchS", "px", scur, "ntag", "pos", () -> {
				C cond0 = pg.emitOp(pg.emitGetter("scur.ntag"), "==", pg.V("ntag"));
				C then0 = pg.emitFunc("nextN", pg.V("px"), pg.emitGetter("scur.value"), pg.emitGetter("scur.cnt"));
				C else0 = pg.emitFunc("matchS", pg.V("px"), pg.emitGetter("scur.prevState"), pg.V("ntag"), pg.V("pos"));
				C inner = pg.emitIf(cond0, then0, else0);
				if (Optional) {
					inner = pg.emitVarDecl(false, "scur", pg.emitFunc("Option.get", pg.V("state")), inner);
				}
				return pg.emitIf(pg.emitIsNull(pg.V(Optional ? "state" : "scur")), pg.emitFail(), inner);
			});
		});
	}

	// Tree

	void loadMain(ParserGenerator<B, C> pg) {

		this.defineLib("AST", () -> {

		});

		this.defineLib("newAST", () -> {
			pg.makeLib("AST");
			this.defFunc(pg, 0, this.T("tree"), "newAST", "tag", "inputs", "pos", "epos", "cnt", () -> {
				return pg.emitFunc("AST.new", pg.V("tag"), pg.V("inputs"), pg.V("pos"), pg.V("epos"), pg.V("cnt"));
			});
		});

		this.defineLib("subAST", () -> {
			this.defFunc(pg, 0, this.T("tree"), "subAST", "tree", "cnt", "label", "child", () -> {
				B block = pg.beginBlock();
				pg.emitStmt(block, pg.emitFunc("AST.set", pg.V("tree"), pg.V("cnt"), pg.V("label"), pg.V("child")));
				pg.Return(block, pg.V("tree"));
				return pg.endBlock(block);
			});
		});

		this.defineLib("check0", () -> {
			this.defFunc(pg, 1, this.T("inputs"), "ckeck0", "inputs", "length", () -> {
				C expr = pg.emitOp(pg.emitArrayLength(pg.V("inputs")), "==", pg.V("length"));
				return pg.emitIf(expr, pg.emitFunc("Byte[]+0", pg.V("inputs"), pg.V("length")), pg.V("inputs"));
			});
		});

		this.defineLib("parse", () -> {
			pg.makeLib("gettag");
			pg.makeLib("newMemos");
			final boolean freeContext = pg.check("freeContext");

			final String[] param1 = { "inputs", "length", "newFunc" };
			final String[] param2 = { "inputs", "length", "newFunc", "setFunc" };
			final String[] param = this.isDefined("TList") ? param1 : param2;

			final C empty = this.isDefined("TList") ? pg.emitFunc("TList.empty") : pg.vInt(0);
			final C initTree = pg.emitApply(pg.V("newFunc"), pg.emitFunc("gettag", pg.vInt(0)), pg.V("inputs"),
					pg.vInt(0), pg.V("length"), empty);
			final C errTree = pg.emitApply(pg.V("newFunc"), pg.emitFunc("gettag", pg.Const("nezerror")), pg.V("inputs"),
					pg.emitGetter("px.headpos"), pg.V("length"), empty);
			final C tokenTree = pg.emitApply(pg.V("newFunc"), pg.emitFunc("gettag", pg.vInt(0)), pg.V("inputs"),
					pg.vInt(0), pg.emitGetter("px.pos"), empty);

			this.defFunc(pg, Apublic, this.T("tree"), "parse", param, () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, true, "tree", initTree);
				// New
				ArrayList<C> args = new ArrayList<>();
				// args.add(pg.emitFunc("check0", pg.V("inputs"),
				// pg.V("length")));
				args.add(pg.V("inputs"));
				args.add(pg.emitConv("Array.start", pg.V("length")));
				args.add(pg.emitConv("Array.start", pg.vInt(0)));
				args.add(pg.emitConv("Array.start", pg.vInt(0)));
				args.add(pg.V("tree"));
				args.add(this.emitNewTreeLog(pg, null, null, pg.V("tree"), null));
				if (this.isDefined("null") && !this.isDefined("paraminit")) {
					args.add(pg.IfNull(pg.V("newFunc"), pg.emitFuncRef("newAST")));
				} else {
					args.add(pg.V("newFunc"));
				}
				if (!this.isDefined("TList")) {
					if (this.isDefined("null") && !this.isDefined("paraminit")) {
						args.add(pg.IfNull(pg.V("setFunc"), pg.emitFuncRef("subAST")));
					} else {
						args.add(pg.V("setFunc"));
					}
				}
				args.add(pg.emitNull("state"));
				args.add(pg.emitFunc("newMemos", pg.V("tree"), pg.vInt(pg.grammar.getMemoPointSize() * 64 + 1)));
				pg.emitVarDecl(block, false, "px", pg.emitNew("NezParserContext", args));
				if (pg.isTreeConstruction()) {
					pg.Assign(block, "tree", pg.emitIf(pg.emitNonTerminal("e0"), pg.emitGetter("px.tree"), errTree));
				} else {
					pg.Assign(block, "tree", pg.emitIf(pg.emitNonTerminal("e0"), tokenTree, errTree));
				}
				if (freeContext) {
					pg.emitStmt(block, pg.emitFunc("freeContext", pg.V("px")));
				}
				pg.Return(block, pg.V("tree"));
				return (pg.endBlock(block));
			});

			final C length = pg.emitArrayLength(pg.V("inputs"));
			if (this.isDefined("TList")) {
				this.defFunc(pg, Apublic, this.T("tree"), "parseText", "text", "newFunc", () -> {
					B block = pg.beginBlock();
					if (this.isDefined("String+0")) {
						pg.emitVarDecl(block, false, "inputs",
								pg.emitConv("String->Byte[]", pg.emitConv("String+0", pg.V("text"))));
						pg.emitVarDecl(block, false, "length", pg.emitOp(length, "-", pg.vInt(1)));
					} else {
						pg.emitVarDecl(block, false, "inputs", pg.emitConv("String->Byte[]", pg.V("text")));
						pg.emitVarDecl(block, false, "length", length);
					}
					pg.Return(block, pg.emitFunc("parse", pg.V("inputs"), pg.V("length"), pg.V("newFunc")));
					return (pg.endBlock(block));
				});
			} else {
				this.defFunc(pg, Apublic, this.T("tree"), "parseText", "text", "newFunc", "setFunc", () -> {
					B block = pg.beginBlock();
					pg.emitVarDecl(block, false, "inputs",
							pg.emitConv("String->Byte[]", pg.emitConv("String+0", pg.V("text"))));
					if (this.isDefined("String+0")) {
						pg.emitVarDecl(block, false, "length", length);
					} else {
						pg.emitVarDecl(block, false, "length", pg.emitOp(length, "-", pg.vInt(1)));
					}
					pg.Return(block,
							pg.emitFunc("parse", pg.V("inputs"), pg.V("length"), pg.V("newFunc"), pg.V("setFunc")));
					return (pg.endBlock(block));
				});
			}
		});
	}

}
