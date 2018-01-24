package blue.origami.transpiler.rule;

import java.util.ArrayList;

import blue.origami.common.ODebug;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.MatchCode;
import blue.origami.transpiler.code.MatchCode.AnyCase;
import blue.origami.transpiler.code.MatchCode.Case;
import blue.origami.transpiler.code.MatchCode.DataCase;
import blue.origami.transpiler.code.MatchCode.ListCase;
import blue.origami.transpiler.code.MatchCode.NameCase;
import blue.origami.transpiler.code.MatchCode.NoneCase;
import blue.origami.transpiler.code.MatchCode.RangeCase;
import blue.origami.transpiler.code.MatchCode.RuleCode;
import blue.origami.transpiler.code.MatchCode.TupleCase;
import blue.origami.transpiler.code.MatchCode.ValuesCase;
import blue.origami.transpiler.type.Ty;

public class MatchExpr implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, AST match) {
		// ODebug.setDebug(true);
		Code targetCode = (match.has(_expr)) ? //
				env.parseCode(env, match.get(_expr)) : null; //
		AST body = match.get(_body);
		RuleCode optionalCase = null;
		ArrayList<RuleCode> l = new ArrayList<>(body.size());
		for (int i = 0; i < body.size(); i++) {
			AST sub = body.get(i);
			Case cse = this.parseCase(env, 0, sub.get(_expr));
			Code bodyCode = env.parseCode(env, sub.get(_body));
			ODebug.trace("%s => %s", cse, bodyCode);
			if (cse instanceof NoneCase) {
				optionalCase = new RuleCode(cse, bodyCode);
			} else {
				l.add(new RuleCode(cse, bodyCode));
			}
		}
		return new MatchCode(targetCode, optionalCase, l.toArray(new RuleCode[0]));
	}

	private Case parseCase(Env env, int a, AST t) {
		String tag = t.getTag().getSymbol();
		switch (tag) {
		case "AnyCase": {
			return new AnyCase();
		}
		case "NoneCase": {
			return new NoneCase();
		}
		case "ValueCase": {
			if (t.has(_list)) {
				Code[] v = new Code[t.get(_list).size()];
				int i = 0;
				for (AST e : t.get(_list)) {
					v[i] = env.parseCode(env, e);
					i++;
				}
				return new ValuesCase(v);
			}
			Code v = env.parseCode(env, t.get(_value));
			return new ValuesCase(v);
		}
		case "RangeCase":
		case "RangeUntilCase": {
			Code start = env.parseCode(env, t.get(_start));
			Code end = env.parseCode(env, t.get(_end));
			return new RangeCase(start, end, !tag.equals("RangeUntilCase"));
		}
		case "TupleCase": {
			Case[] l = new Case[t.size()];
			assert (t.size() > 1) : "tuple " + t;
			int i = 0;
			for (AST e : t) {
				l[i] = this.parseCase(env, 0, e);
				i++;
			}
			return new TupleCase(l);
		}
		case "ListCase": {
			Case[] l = new Case[t.size()];
			int i = 0;
			for (AST e : t) {
				l[i] = this.parseCase(env, 0, e);
				i++;
			}
			return new ListCase(l);
		}
		case "DataCase": {
			Case[] l = new Case[t.size()];
			int i = 0;
			for (AST e : t) {
				l[i] = this.parseCase(env, 0, e);
				String name = l[i].getName();
				Ty hint = env.findNameHint(name);
				if (hint == null) {
					throw new ErrorCode(e, TFmt.no_type_hint__YY1, name);
				}
				// l[i].setNameType(hint.getType());
				i++;
			}
			return new DataCase(l);
		}
		case "NameCase": {
			// ODebug.trace("NameCase %s", t);
			String name = t.getStringAt(_name, "");
			String suffix = t.getStringAt(_suffix, "");
			if (t.has(_cond)) {
				Case caseCode = this.parseCase(env, 0, t.get(_cond));
				caseCode.setNameSuffix(name, suffix);
				return caseCode;
			}
			if (t.has(_where)) {
				AST where = t.get(_where);
				return new NameCase(name, suffix, where.getTag().getSymbol(), env.parseCode(env, where.get(_right)));
			}
			return new NameCase(name, suffix, "", null);
		}
		default:
			throw new ErrorCode(t, TFmt.undefined_syntax__YY1, t.getTag());
		}
	}

	// ----------------------

}
