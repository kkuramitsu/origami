package origami.nezcc2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

import blue.origami.Version;
import blue.origami.common.OFactory;
import blue.origami.common.OOption;
import blue.origami.common.SourcePosition;
import blue.origami.main.MainOption;
import origami.tcode.TCode;
import origami.tcode.TCodeBuilder;
import origami.tcode.TSyntaxMapper;

public class NezCC extends TCodeBuilder implements OFactory<NezCC> {
	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;
	final static int EMPTY = 1 << 3;
	private int mask = POS;

	@Override
	public Class<?> keyClass() {
		return NezCC.class;
	}

	@Override
	public NezCC clone() {
		return new NezCC();
	}

	TSyntaxMapper syntax = new TSyntaxMapper();

	@Override
	public void init(OOption options) {
		String file0 = options.stringValue(MainOption.GrammarFile, "parser.opeg");
		String base = SourcePosition.extractFileBaseName(file0);
		this.syntax.defineSyntax("base", base);
		this.syntax.defineSyntax("nezcc", "nezcc/2.0");
		this.syntax.defineSyntax("space", " ");
		this.syntax.defineSyntax(" ", " ");
		this.syntax.defineSyntax("\\n", "\n");
		this.syntax.defineSyntax("\\t", "\t");

		if (options.is(MainOption.TreeConstruction, true)) {
			this.mask |= TREE;
		}
		String file = options.stringValue(MainOption.FromFile, "chibi.nezcc");
		int p = file.indexOf("+");
		int p2 = file.indexOf("-");
		if (p > 0 || p2 > 0) {
			p = (p != -1 && p2 != -1) ? Math.min(p, p2) : Math.max(p, p2);
			String s = file.substring(p).replace(".nezcc", "");
			file = file.substring(0, p) + ".nezcc";
			s = s.replace("+", ",").replace("-", ",!").substring(1);
			this.syntax.defineSyntax("localoptions", s);
		}
		if (!new File(file).isFile()) {
			file = Version.ResourcePath + "/nezcc2/" + file;
		}
		this.syntax.importSyntaxFile(file);
	}

	static TCode defun(String name, String p0, String body) {
		return funcDecl(var(name), params(p0), p(body));
	}

	static TCode defun(String name, String p0, String p1, String body) {
		return funcDecl(var(name), params(p0, p1), p(body));
	}

	static TCode defun(String name, String p0, String p1, TCode body) {
		return funcDecl(var(name), params(p0, p1), body);
	}

	static TCode defun(String name, String p0, String p1, String p2, String body) {
		return funcDecl(var(name), params(p0, p1, p2), p(body));
	}

	static TCode defun(String name, String p0, String p1, String p2, String p3, String body) {
		return funcDecl(var(name), params(p0, p1, p2, p3), p(body));
	}

	static TCode defun(String name, String p0, String p1, String p2, String p3, String p4, String body) {
		return funcDecl(var(name), params(p0, p1, p2, p3, p4), p(body));
	}

	static TCode defun(String name, String p0, String p1, String p2, String p3, String p4, String p5, String body) {
		return funcDecl(var(name), params(p0, p1, p2, p3, p4, p5), p(body));
	}

	boolean isDefined(String key) {
		return false;
	}

	// String getDefined(String key) {
	// if (this.isDefined("D" + key)) {
	// return this.formatMap.get("D" + key);
	// }
	// try {
	// Field f = this.getClass().getField(key);
	// return ((Supplier<TCode>) f.get(this)).get().toString();
	// } catch (Exception e) {
	// e.printStackTrace();
	// return "TODO " + key + " => " + e;
	// }
	// }

	public Supplier<TCode> neof = () -> {
		return defun("neof", "px", "px.pos < px.length");
	};

	public Supplier<TCode> succ = () -> {
		return defun("succ", "px", "true");
	};

	public Supplier<TCode> fail = () -> {
		return defun("fail", "px", "false");
	};

	public Supplier<TCode> mnext1 = () -> {
		return defun("mnext1", "px", "px.pos = px.pos + 1; true");
	};

	public Supplier<TCode> mmov = () -> {
		return defun("mmov", "px", "pos", "px.pos = px.pos + pos; px.pos < px.length");
	};

	public Supplier<TCode> match2 = () -> {
		return defun("match2", "px", "ch", "ch2", "px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2");
	};

	public Supplier<TCode> match3 = () -> {
		return defun("match3", "px", "ch", "ch2", "ch3",
				"px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2 && px.inputs[px.pos+2] == ch3");
	};

	public Supplier<TCode> match4 = () -> {
		return defun("match4", "px", "ch", "ch2", "ch3", "ch4",
				"px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2 && px.inputs[px.pos+2] == ch3 && px.inputs[px.pos+3] == ch4");
	};

	public Supplier<TCode> matchmany = () -> {
		return defun(/* rec */"matchmany", "stext", "spos", "etext", "epos", "slen",
				"slen == 0 || (stext[spos] == etext[epos] && matchmany(stext, spos+1, etext, epos+1, slen-1))");
	};

	public Supplier<TCode> mback1 = () -> {
		return defun("mback1", "px", "pos", "px.pos = errpos!(pos); true");
	};

	public Supplier<TCode> mback2 = () -> {
		return defun("mback2", "px", "tree", "px.tree = tree; true");
	};

	public Supplier<TCode> mback3 = () -> {
		return defun("mback3", "px", "pos", "tree", "px.pos = errpos!(pos); px.tree = tree; true");
	};

	public Supplier<TCode> mback4 = () -> {
		return defun("mback4", "px", "state", "px.state = state; true");
	};

	public Supplier<TCode> mback7 = () -> {
		return defun("mback7", "px", "pos", "tree", "state",
				"px.pos = errpos!(pos); px.tree = tree; px.state = state; true");
	};

	public Supplier<TCode> maybe1 = () -> {
		return defun("maybe1", "px", "pe", "let pos = px.pos; pe(px) || mback1(px, pos)");
	};

	public Supplier<TCode> maybe3 = () -> {
		return defun("maybe3", "px", "pe", "let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree)");
	};

	public Supplier<TCode> maybe7 = () -> {
		return defun("maybe7", "px", "pe",
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state)");
	};

	public Supplier<TCode> or1 = () -> {
		return defun("or1", "px", "pe", "pe2", "let pos = px.pos; pe(px) || mback1(px, pos) && pe2(px)");
	};

	public Supplier<TCode> or3 = () -> {
		return defun("or3", "px", "pe", "pe2",
				"let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree) && pe2(px)");
	};

	public Supplier<TCode> or7 = () -> {
		return defun("or7", "px", "pe", "pe2",
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state) && pe2(px)");
	};

	public Supplier<TCode> oror1 = () -> {
		return defun("oror1", "px", "pe", "pe2", "pe3",
				"let pos = px.pos; pe(px) || mback1(px, pos) && pe2(px) || mback1(px, pos) && pe3(px)");
	};

	public Supplier<TCode> oror3 = () -> {
		return defun("oror3", "px", "pe", "pe2", "pe2",
				"let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree) && pe2(px) || mback3(px, pos, tree) && pe3(px)");
	};

	public Supplier<TCode> oror7 = () -> {
		return defun("oror7", "px", "pe", "pe2", "pe3",
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state) && epe(px) || mback7(px, pos, tree, state) && ee2(px)");
	};

	public Supplier<TCode> ororor1 = () -> {
		return defun("ororor1", "px", "pe", "pe2", "pe3", "pe4",
				"let pos = px.pos; pe(px) || mback1(px, pos) && pe2(px) || mback1(px, pos) && pe3(px) || mback1(px, pos) && pe4(px)");
	};

	public Supplier<TCode> ororor3 = () -> {
		return defun("ororor3", "px", "pe", "pe2", "pe3", "pe4",
				"let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree) && pe2(px) || mback3(px, pos, tree) && pe3(px) || mback3(px, pos, tree) && pe4(px)");
	};

	public Supplier<TCode> ororor7 = () -> {
		return defun("ororor7", "px", "pe", "pe2", "pe3", "pe4",
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state) && pe2(px) || mback7(px, pos, tree, state) && pe3(px) || mback7(px, pos, tree, state) && pe4(px)");
	};

	public Supplier<TCode> many1 = () -> {
		if (this.isDefined("while")) {
			return defun("many1", "px", "pe", "let pos = px.pos; while(pe(px), {pos = px.pos}, mback1(px, pos))");
		}
		return defun(/* rec */"many1", "px", "pe", "let pos = px.pos; pe(px) ? many1(px, pe) ? mback1(px, pos)");
	};

	public Supplier<TCode> many3 = () -> {
		if (this.isDefined("while")) {
			return defun("many3", "px", "pe",
					"let pos = px.pos; let tree = px.tree; while(pe(px), {pos = px.pos; tree = px.tree}, mback3(px, pos, tree))");
		}
		return defun(/* rec */"many3", "px", "pe",
				"let pos = px.pos; let tree = px.tree; pe(px) ? many3(px, pe) ? mback3(px, pos, tree)");
	};

	public Supplier<TCode> many7 = () -> {
		if (this.isDefined("while")) {
			return defun("many7", "px", "pe",
					"let pos = px.pos; let tree = px.tree; let state = px.state; while(pe(px), {pos = px.pos; tree = px.tree; state = px.state}, mback7(px, pos, tree, state))");
		}
		return defun(/* rec */"many7", "px", "pe",
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) ? many7(px, pe) ? mback7(px, pos, tree, state)");
	};

	public Supplier<TCode> many9 = () -> {
		if (this.isDefined("while")) {
			return defun("many9", "px", "pe",
					"let pos = px.pos; while(pe(px) && pos < px.pos, {pos = px.pos}, mback1(px, pos))");
		}
		return defun(/* rec */"many9", "px", "pe",
				"let pos = px.pos; pe(px) && pos < px.pos ? many9(px, pe) ? mback1(px, pos)");
	};

	public Supplier<TCode> many12 = () -> {
		if (this.isDefined("while")) {
			return defun("many12", "px", "pe",
					"let pos = px.pos; let tree = px.tree; while(pe(px) && pos < px.pos, {pos = px.pos; tree = px.tree}, mback3(px, pos, tree))");
		}
		return defun(/* rec */"many12", "px", "pe",
				"let pos = px.pos; let tree = px.tree; pe(px) && pos < px.pos ? many12(px, pe) ? mback3(px, pos, tree)");
	};

	public Supplier<TCode> many16 = () -> {
		if (this.isDefined("while")) {
			return defun("many16", "px", "pe",
					"let pos = px.pos; let tree = px.tree; let state = px.state; while(pe(px) && pos < px.pos, {pos = px.pos; tree = px.tree; state = px.state}, mback7(px, pos, tree, state))");
		}
		return defun(/* rec */"many16", "px", "pe",
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) && pos < px.pos ? many16(px, pe) ? mback7(px, pos, tree, state)");
	};

	public Supplier<TCode> manyany = () -> {
		if (this.isDefined("while")) {
			return defun("manyany", "px", "while(px.pos < px.length, {px.pos = px.pos + 1}, true)");
		}
		return defun(/* rec */"manyany", "px", "px.pos < px.length && mnext1(px) && manyany(px, e)");
	};

	public Supplier<TCode> manychar = () -> {
		TCode index = p("px.inputs[px.pos]");
		if (this.isDefined("Obits32")) {
			index = apply("bits32", "bm", index);
		} else {
			index = p("$0[unsigned!($1)]", "bm", index);
		}
		if (this.isDefined("while")) {
			return defun("manychar", "px", "bm", p("while($0, {px.pos = px.pos + 1}, true)", index));
		}
		return defun(/* rec */"manychar", "px", "bm", p("$0 && mnext1(px) && manychar(px, bm)", index));
	};

	public Supplier<TCode> manystr = () -> {
		if (this.isDefined("while")) {
			return defun("manystr", "px", "stext", "slen",
					"while(matchmany(px.inputs, px.pos, stext, 0, slen), {px.pos = px.pos + slen}, true)");
		}
		return defun("manystr", "px", "stext", "slen",
				"matchmany(px.inputs, px.pos, stext, 0, slen) && mmov(px, slen) && manystr(px, stext, slen)");
	};

	public Supplier<TCode> and1 = () -> {
		return defun("and1", "px", "pe", "let pos = px.pos; pe(px) && mback1(px, pos)");
	};

	public Supplier<TCode> not1 = () -> {
		return defun("not1", "px", "pe", "let pos = px.pos; !pe(px) && mback1(px, pos)");
	};

	public Supplier<TCode> not3 = () -> {
		return defun("not3", "px", "pe", "let pos = px.pos; let tree = px.tree; !pe(px) && mback3(px, pos, tree)");
	};

	public Supplier<TCode> not7 = () -> {
		return defun("not7", "px", "pe",
				"let pos = px.pos; let tree = px.tree; let state = px.state; !pe(px) && mback7(px, pos, tree, state)");
	};

	public Supplier<TCode> minc = () -> {
		return defun("minc", "px", "let pos = px.pos; px.pos = px.pos + 1; pos");
	};

	/* Tree Construction */

	public Supplier<TCode> mtree = () -> {
		return defun("mtree", "px", "tag", "spos", "epos",
				"px.tree = ctree(tag, px.inputs, spos, epos, px.tree); true");
	};

	public Supplier<TCode> mlink = () -> {
		return defun("mlink", "px", "tag", "child", "prev", "px.tree = clink(tag, child, prev); true");
	};

	public Supplier<TCode> newtree = () -> {
		return defun("newtree", "px", "spos", "pe", "tag", "epos",
				"let pos = px.pos; px.tree = EmptyTree; pe(px) && mtree(px, tag, pos+spos, px.pos+epos)");
	};

	public Supplier<TCode> foldtree = () -> {
		return defun("foldtree", "px", "spos", "label", "pe", "tag", "epos",
				"let pos = px.pos; mlink(px, label, px.tree, EmptyTree) && pe(px) && mtree(px, tag, pos+spos, px.pos+epos)");
	};

	public Supplier<TCode> linktree = () -> {
		return defun("linktree", "px", "tag", "pe", "let tree = px.tree; pe(px) && mlink(px, tag, px.tree, tree)");
	};

	public Supplier<TCode> tagtree = () -> {
		return defun("tagtree", "px", "tag", "mlink(px, tag, EmptyTree, px.tree)");
	};

	public Supplier<TCode> detree = () -> {
		return defun("detree", "px", "pe", "let tree = px.tree; pe(px) && mback3(px, px.pos, tree)");
	};

	public Supplier<TCode> mconsume1 = () -> {
		return defun("mconsume1", "px", "memo", "px.pos = memo.mpos; memo.matched");
	};

	public Supplier<TCode> mconsume3 = () -> {
		return defun("mconsume3", "px", "memo", "px.pos = memo.mpos; px.tree = memo.mtree; memo.matched");
	};

	public Supplier<TCode> mconsume7 = () -> {
		return defun("mconsume7", "px", "memo",
				"px.pos = memo.mpos; px.tree = memo.mtree; px.state = memo.mstate; memo.matched");
	};

	public Supplier<TCode> mstore1 = () -> {
		return defun("mstore1", "px", "memo", "key", "pos", "matched",
				"memo.key = key; memo.mpos = pos; memo.matched = matched; matched");
	};

	public Supplier<TCode> mstore3 = () -> {
		return defun("mstore3", "px", "memo", "key", "pos", "matched",
				"memo.key = key; memo.mpos = pos; memo.mtree = px.tree; memo.matched = matched; matched");
	};

	public Supplier<TCode> mstore7 = () -> {
		return defun("mstore7", "px", "memo", "key", "pos", "matched",
				"memo.key = key; memo.mpos = pos; memo.mtree = px.tree; memo.mstate = px.state; memo.matched = matched; matched");
	};

	public Supplier<TCode> getkey = () -> {
		return defun(/* type key */"getkey", "pos", "mp", "pos * memosize + mp");
	};

	public Supplier<TCode> getmemo = () -> {
		return defun(/* type memo */"getmemo", "px", "key", "px.memos[keyindex!(key % memolen)]");
	};

	public Supplier<TCode> memo1 = () -> {
		return defun("memo1", "px", "mp", "pe",
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); (memo.key == key) ? mconsume1(px, memo) ? mstore1(px, memo, key, pos, pe(px))");
	};

	public Supplier<TCode> memo3 = () -> {
		return defun("memo3", "px", "mp", "pe",
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); (memo.key == key) ? mconsume3(px, memo) ? mstore3(px, memo, key, pos, pe(px))");
	};

	public Supplier<TCode> memo7 = () -> {
		return defun("memo7", "px", "mp", "pe",
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); (memo.key == key) ? mconsume7(px, memo) ? mstore7(px, memo, key, pos, pe(px))");
	};

	public Supplier<TCode> scope4 = () -> {
		return defun("scope4", "px", "pe", "let state = px.state; pe(px) && mback4(px, state)");
	};

	public Supplier<TCode> mstate = () -> {
		return defun("mstate", "px", "ns", "pos", "px.state = cstate(ns, pos, px.pos, px.state); true");
	};

	public Supplier<TCode> getstate = () -> {
		return defun(/* rec *//* type state */"getstate", "state", "ns",
				"(state == EmptyState || state.ns == ns) ? state ? getstate(state.sprev, ns)");
	};

	public Supplier<TCode> symbol4 = () -> {
		return defun("symbol4", "px", "ns", "pe", "let pos = px.pos; pe(px) && mstate(px, ns, pos)");
	};

	public Supplier<TCode> smatch4 = () -> {
		return defun("smatch4", "px", "ns",
				"let state = getstate(px.state, ns); state != EmptyState && matchmany(px.inputs, px.pos, px.inputs, state.spos, state.slen) && mmov(px, state.slen)");
	};

	public Supplier<TCode> sexists4 = () -> {
		return defun("sexists4", "px", "ns", "getstate(px.state, ns) != EmptyState");
	};

	public Supplier<TCode> rexists4 = () -> {
		return defun(/* rec */"rexists4", "px", "state", "stext", "slen",
				"state != EmptyState && (state.slen == slen && matchmany(stext, 0, px.inputs, state.spos, slen) || rexists4(px, getstate(state.sprev, state.ns), stext, slen))");
	};

	public Supplier<TCode> sequals4 = () -> {
		return defun("sequals", "px", "ns", "pe",
				"let pos = px.pos; let state = getstate(px.state, ns); state != EmptyState && pe(px) && state.slen == px.pos - pos && matchmany(px.inputs, pos, px.inputs, state.spos, px.pos - pos)");
	};

	public Supplier<TCode> smany = () -> {
		return defun(/* rec */"smany", "px", "state", "pos", "slen",
				"state != EmptyState && ((state.slen == slen && matchmany(px.inputs, pos, px.inputs, state.spos, slen)) || smany(px, getstate(state.sprev, state.ns), pos, slen))");
	};

	public Supplier<TCode> scontains4 = () -> {
		return defun("scontains", "px", "ns", "pe",
				"let pos = px.pos; pe(px) && smany(px, getstate(px.state, ns), pos, px.pos - pos)");
	};

	// curry function

	public Supplier<TCode> cadd = () -> {
		return defun("cadd", "pe", "pe2", "\\px Let(pe(px), pe2(px') ?? Fail)");
	};

	public Supplier<TCode> cc1 = () -> {
		return defun("cc1", "ch", "\\px px.inputs[px.pos] == ch ? Succ(pos, pos+1) ? Fail");
	};

	public Supplier<TCode> cc2 = () -> {
		return defun("cc2", "ch", "ch2",
				"\\px px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2 ? Succ(pos, pos+2) ? Fail");
	};

	public Supplier<TCode> cor1 = () -> {
		return defun("cor1", "pe", "pe2", "\\px Let(pos, pe(px), pxn ?? Back(pos, pos, pe2(px))");
	};

	public Supplier<TCode> cmany1 = () -> {
		return defun("cmany1", "pe", "\\px Let(pos, pe(px), Rep(pos, pos) ?? Succ(pos, pos))");
	};

	public Supplier<TCode> cand1 = () -> {
		return defun("cand1", "pe", "\\px Let(pos, pe(px), Succ(pos, pos) ?? Fail)");
	};

	public Supplier<TCode> cnot1 = () -> {
		return defun("cnot1", "pe", "\\px Let(pos, pe(px), Fail ?? Succ(pos, pos))");
	};

	public Supplier<TCode> ctree1 = () -> {
		return defun("ctree1", "pe", "tag", "\\px Let(pos, pe(px), Succ(tree, ctree) ?? Fail)");
	};

	public Supplier<TCode> clink1 = () -> {
		return defun("clink1", "pe", "tag", "\\px Let(tree, pe(px), Succ(tree, clink) ?? Fail)");
	};

	public Supplier<TCode> ctag1 = () -> {
		return defun("ctag1", "tag", "\\px Succ(tree, clink)");
	};

	private static String[] runtimeFuncs1 = { //
			"mnext1", "mmov", "neof", "succ", "fail", //
			"match2", "match3", "match4", "matchmany", //
			"mback1", "mback2", "mback3", "mback4", "mback7", //
			"maybe1", "maybe3", "maybe7", //
			"or1", "or3", "or7", //
			"oror1", "oror3", "oror7", //
			"ororor1", "ororor3", "ororor7", //
			"many1", "many3", "many7", "manyany", "manychar", "manystr", //
			"many9", "many12", "many16", //
			"and1", "not1", "not3", "not7", //
			"mtree", "mlink", "newtree", "foldtree", "linktree", "tagtree", //
			"getkey", "getmemo", //
			"mconsume1", "mconsume3", "mconsume7", //
			"mstore1", "mstore3", "mstore7", //
			"memo1", "memo3", "memo7", //
			"mstate", "symbol4", "scope4", "getstate", "sreset4", "sremove", //
			"smatch4", "sexists4", "rexists4", "sequals4", "smany", "scontains4",//
	};

	// public ENode dispatch(ENode eJumpIndex, List<ENode> exprs) {
	// return new Dispatch(eJumpIndex, exprs);
	// }

	// class Dispatch extends ENode {
	// ENode eJumpIndex;
	// ENode[] exprs;
	//
	// public Dispatch(ENode eJumpIndex, List<ENode> exprs) {
	// this.eJumpIndex = eJumpIndex;
	// this.exprs = exprs.toArray(new ENode[exprs.size()]);
	// }
	//
	// @Override
	// public ENode ret() {
	// if (!this.isDefined("Ojumptable")) {
	// for (int i = 0; i < this.exprs.length; i++) {
	// this.exprs[i] = this.exprs[i].ret();
	// }
	// return this;
	// }
	// return super.ret();
	// }
	//
	// @Override
	// public ENode deret() {
	// for (int i = 0; i < this.exprs.length; i++) {
	// this.exprs[i] = this.exprs[i].deret();
	// }
	// return this;
	// }
	//
	// @Override
	// void emit(Writer w) {
	// if (this.isDefined("Ojumptable")) {
	// new Apply(new GetIndex(new FuncValue(this.exprs), this.eJumpIndex), new
	// Var("px")).emit(w);
	// return;
	// }
	// if (this.isDefined("switch")) {
	// w.format(this.formatOf("switch"), this.eJumpIndex);
	// w.incIndent();
	// for (int i = 0; i < this.exprs.length; i++) {
	// w.format(this.formatOf("case", "\t| %s => %s\n"), i, this.exprs[i]);
	// }
	// if (this.isDefined("default")) {
	// w.format(this.formatOf("default", "\t| _ => %s\n"), this.exprs[0]);
	// }
	// w.decIndent();
	// w.format(this.formatOf("end switch", "end", ""));
	// return;
	// } else {
	// this.deret();
	// ENode tail = this.exprs[0];
	// for (int i = this.exprs.length - 1; i > 0; i--) {
	// tail = new IfExpr(NezCC2.this.p("pos == $0", i), this.exprs[i], tail);
	// if (i > 0) {
	// tail = new Unary("group", tail);
	// }
	// }
	// tail = new LetIn("pos", this.eJumpIndex).add(tail);
	// tail.ret().emit(w);
	// }
	// }
	//
	// class FuncValue extends ENode {
	// ENode[] exprs;
	//
	// FuncValue(ENode[] data) {
	// this.exprs = data;
	// }
	//
	// @Override
	// void emit(Writer w0) {
	// Writer w = new Writer();
	// w.push(this.formatOf("array", "["));
	// for (int i = 0; i < this.exprs.length; i++) {
	// if (i > 0) {
	// w.push(this.formatOf("delim array", "delim", " "));
	// }
	// w.push(this.exprs[i]);
	// }
	// w.push(this.formatOf("end array", "]"));
	// w0.format(this.formatOf("constname", "%s",
	// NezCC2.this.constName(this.typeOf("jumptbl"), "jumptbl",
	// this.exprs.length, w.toString(), null)));
	// }
	// }
	//
	// }

	// ad hoc parser

	static int flatIndexOf(String expr, char c) {
		int level = 0;
		for (int i = 0; i < expr.length(); i++) {
			char c0 = expr.charAt(i);
			if (c0 == c && level == 0) {
				return i;
			}
			if (c0 == '(' || c0 == '[' || c0 == '{') {
				level++;
			}
			if (c0 == ')' || c0 == ']' || c0 == '}') {
				level--;
			}
		}
		return -1;
	}

	static int flatIndexOf(String expr, char c, char c2) {
		int level = 0;
		for (int i = 0; i < expr.length() - 1; i++) {
			char c0 = expr.charAt(i);
			if (c0 == c && expr.charAt(i + 1) == c2 && level == 0) {
				return i;
			}
			if (c0 == '(' || c0 == '[' || c0 == '{') {
				level++;
			}
			if (c0 == ')' || c0 == ']' || c0 == '}') {
				level--;
			}
		}
		return -1;
	}

	static String[] flatSplit(String expr, char c) {
		ArrayList<String> l = new ArrayList<>();
		int p = flatIndexOf(expr, c);
		while (p != -1) {
			l.add(expr.substring(0, p).trim());
			expr = expr.substring(p + 1);
			p = flatIndexOf(expr, c);
		}
		l.add(expr.trim());
		return l.toArray(new String[l.size()]);
	}

	static String[] flatSplit2(String expr, char c) {
		int p = flatIndexOf(expr, c);
		assert (p != -1);
		return new String[] { expr.substring(0, p).trim(), expr.substring(p + 1) };
	}

	static TCode unary(String expr, final TCode... args) {
		expr = expr.trim();
		// this.dump(expr, args);
		if (expr.startsWith("!")) {
			return unary("!", p_(expr.substring(1), args));
		}
		if (expr.startsWith("{") && expr.endsWith("}")) {
			return p_(expr.substring(1, expr.length() - 1), args);
		}
		if (expr.endsWith("]")) {
			int pos = flatIndexOf(expr, '[');
			TCode b = unary(expr.substring(0, pos), args);
			TCode e = p_(expr.substring(pos + 1, expr.length() - 1), args);
			return getindex(b, e);
		}
		if (expr.endsWith(")")) {
			int pos = flatIndexOf(expr, '(');
			if (pos > 0) {
				String fname = expr.substring(0, pos).trim();
				String[] tokens = flatSplit(expr.substring(pos + 1, expr.length() - 1), ',');
				TCode[] a = Arrays.stream(tokens).map(s -> {
					return p_(s.trim(), args);
				}).toArray(TCode[]::new);
				if (fname.equals("while")) {
					assert a.length == 3;
					return whileStmt(a[0], a[1], a[2]);
				}
				if (fname.endsWith("!")) {
					assert a.length == 1;
					return macro(fname.substring(0, fname.length() - 1), a[0]);
				}
				return apply(fname, a);
			}
			return unary("group", p_(expr.substring(1, expr.length() - 1), args));
		}
		int pos = flatIndexOf(expr, '.');
		if (pos > 0) {
			return getter(var(expr.substring(0, pos)), expr.substring(pos + 1));
		}
		if (expr.startsWith("$")) {
			return args[Integer.parseInt(expr.substring(1))];
		}
		if (expr.startsWith("^")) {
			return funcRef(expr.substring(1));
		}
		if (expr.startsWith("'")) {
			return var(expr.substring(1));
		}
		if (expr.length() > 0 && Character.isDigit(expr.charAt(0))) {
			return i(Integer.parseInt(expr));
		}
		return var(expr);
	}

	@FunctionalInterface
	static interface Bin {
		TCode apply(TCode e, TCode e2);
	}

	static TCode bin(String expr, String op, TCode[] args, Bin f) {
		assert (op.length() < 3);
		int pos = op.length() == 2 ? flatIndexOf(expr, op.charAt(0), op.charAt(1)) : flatIndexOf(expr, op.charAt(0));
		if (pos > 0) {
			return f.apply(p_(expr.substring(0, pos).trim(), args), p_(expr.substring(pos + op.length()).trim(), args));
		}
		return null;
	}

	static void dump(String expr, final TCode[] args) {
		switch (args.length) {
		case 0:
			// System.err.printf("@0'%s'\n", expr);
			break;
		case 1:
			System.err.printf("@1'%s' %s :%s\n", expr, args[0], args[0].getClass().getSimpleName());
			break;
		case 2:
			System.err.printf("@2'%s' %s :%s   %s :%s\n", expr, args[0], args[0].getClass().getSimpleName(), args[1],
					args[1].getClass().getSimpleName());
			break;
		case 3:
			System.err.printf("@3'%s' %s :%s   %s :%s   %s :%s\n", expr, args[0], args[0].getClass().getSimpleName(),
					args[1], args[1].getClass().getSimpleName(), args[2], args[2].getClass().getSimpleName());
			break;
		case 4:
			System.err.printf("@4'%s' %s :%s   %s :%s   %s :%s   %s :%s\n", expr, args[0],
					args[0].getClass().getSimpleName(), args[1], args[1].getClass().getSimpleName(), args[2],
					args[2].getClass().getSimpleName(), args[3], args[3].getClass().getSimpleName());
			break;
		default:
			System.err.printf("@%d'%s' ...\n", expr, args.length);
			break;

		}
	}

	static TCode p_(String expr, final TCode[] args) {
		expr = expr.trim();
		// dump(expr, args);
		TCode p = null;
		// let n = expr ; ...
		int pos = flatIndexOf(expr, ';');
		if (pos > 0) {
			String[] tokens = flatSplit(expr, ';');
			TCode[] a = Arrays.stream(tokens).map(s -> p_(s.trim(), args)).toArray(TCode[]::new);
			return block(a);
		}
		if (expr.startsWith("let ")) {
			String[] t = flatSplit2(expr.substring(4), '=');
			return letIn(t[0], p_(t[1], args));
		}
		p = bin(expr, "==", args, (e, e2) -> infix(e, "==", e2));
		if (p != null) {
			return p;
		}
		p = bin(expr, "!=", args, (e, e2) -> infix(e, "!=", e2));
		if (p != null) {
			return p;
		}
		p = bin(expr, "= ", args, (e, e2) -> infix(e, "=", e2));
		if (p != null) {
			return p;
		}
		pos = flatIndexOf(expr, '?');
		if (pos > 0) {
			String[] tokens = expr.split("\\?");
			TCode[] a = Arrays.stream(tokens).map(s -> p_(s.trim(), args)).toArray(TCode[]::new);
			return ifExpr(a[0], a[1], a[2]);
		}
		p = bin(expr, "||", args, (e, e2) -> infix(e, "||", e2));
		if (p != null) {
			return p;
		}
		p = bin(expr, "&&", args, (e, e2) -> infix(e, "&&", e2));
		if (p != null) {
			return p;
		}
		p = bin(expr, "<", args, (e, e2) -> infix(e, "<", e2));
		if (p != null) {
			return p;
		}
		p = bin(expr, "+", args, (e, e2) -> infix(e, "+", e2));
		if (p != null) {
			return p;
		}
		// p = bin(expr, "*", args, (e, e2) -> new Infix(e, "*", e2));
		// if (p != null) {
		// return p;
		// }
		return unary(expr, args);
	}

	static TCode[] filter(Object... args) {
		if (args instanceof TCode[]) {
			return (TCode[]) args;
		}
		return Arrays.stream(args).map(o -> {
			// if (o instanceof blue.origami.common.Symbol) {
			// return new SymbolValue(o.toString());
			// }
			// if (o instanceof BitChar) {
			// return new BitCharValue((BitChar) o);
			// }
			// if (o instanceof byte[]) {
			// return new IndexValue((byte[]) o);
			// }
			// if (o == null) {
			// return new Var("EmptyTag");
			// }
			return var(o.toString() + ":" + o.getClass().getSimpleName());
		}).toArray(TCode[]::new);
	}

	static TCode p(String expr, Object... args) {
		return p_(expr, filter(args));
	}

	public static TCode apply(String func, Object... args) {
		assert (func != null);
		// if (func == null) {
		// return new Lambda((String) args[0], (TCode) args[1]);
		// }
		return new TCode(null, func, filter(args));
	}

}
