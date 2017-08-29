package blue.origami.transpiler.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.transpiler.rule.Symbols;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.StringCombinator;

public class MatchCode extends CodeN implements ParseRule, Symbols {

	public MatchCode() {
	}

	Code targetCode;

	MatchCode(Code targetCode, boolean isOptional, RuleCode... cases) {
		super(Ty.tAuto, cases);
		this.targetCode = targetCode;
	}

	@Override
	public Ty getType() {
		return this.args[0].getType();
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		for (int i = 0; i < this.size(); i++) {
			this.args[i].asType(env, ret);
		}
		return this.castType(env, ret);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("match (");
		StringCombinator.append(sb, this.targetCode);
		sb.append(") ");
		Arrays.stream(this.args).forEach((r) -> {
			r.strOut(sb);
		});
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushMatch(env, this);
	}

	RuleCode get(int index) {
		return (RuleCode) this.args[index];
	}

	public Code desugarIfCode() {
		int last = this.size() - 1;
		RuleCode r = this.get(last);
		Code elseCode = r.thenCode();
		for (int i = last - 1; i >= 0; i--) {
			r = this.get(i);
			elseCode = new IfCode(r.condCode(), r.thenCode(), elseCode);
		}
		return elseCode;
	}

	@Override
	public Code apply(TEnv env, Tree<?> match) {
		Code targetCode = (match.has(_expr)) ? //
				env.parseCode(env, match.get(_expr)) : //
				this.firstArgument(env);
		Tree<?> body = match.get(_body);
		RuleCode[] rules = new RuleCode[body.size() + 1];
		int c = 1;
		boolean isOptional = false;
		for (int i = 0; i < body.size(); i++) {
			Tree<?> sub = body.get(i);
			Case cse = this.parseCase(env, sub);
			Code bodyCode = env.parseCode(env, sub.get(_body));
			if (cse instanceof NoneCase) {
				rules[0] = new RuleCode(cse, bodyCode);
				isOptional = true;
			} else {
				rules[c] = new RuleCode(cse, bodyCode);
				if (c > 1 && !rules[1].same(rules[c])) {
					throw new ErrorCode(sub, TFmt.different_pattern);
				}
				c++;
			}
		}
		rules = isOptional ? TArrays.rtrim2(rules) : TArrays.ltrim2(rules);
		for (RuleCode r : rules) {
			r.desugar(targetCode);
		}
		MatchCode matchCode = new MatchCode(targetCode, isOptional, rules);
		System.out.println(":::::" + matchCode);
		System.out.println(":::::" + matchCode.desugarIfCode());
		return matchCode.desugarIfCode();
	}

	private Code firstArgument(TEnv env) {
		FunctionContext fcx = env.get(FunctionContext.class);
		if (fcx != null && fcx.size() > 0) {
			return fcx.getFirstArgument().newCode(null);
		}
		throw new ErrorCode(TFmt.required_first_argument);
	}

	private Case parseCase(TEnv env, Tree<?> tbody) {
		Tree<?> t = tbody.get(_expr);
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
				Code[] v = new Code[t.size(_list, 0)];
				int i = 0;
				for (Tree<?> e : t.get(_list)) {
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
		case "ListCase": {
			Case[] l = new Case[t.size()];
			int i = 0;
			for (Tree<?> e : t) {
				l[i] = this.parseCase(env, e);
				i++;
			}
			return new ListCase(l);
		}
		case "DataCase": {
			Case[] l = new Case[t.size()];
			int i = 0;
			for (Tree<?> e : t) {
				l[i] = this.parseCase(env, e);
				String name = l[i].name;
				NameHint hint = env.findGlobalNameHint(env, name);
				if (hint == null) {
					throw new ErrorCode(e, TFmt.undefined_name__YY0, name);
				}
				l[i].setNameType(hint.getType());
				i++;
			}
			return new DataCase(l);
		}
		case "NameCase": {
			String name = t.getStringAt(_name, "");
			String suffix = t.getStringAt(_suffix, "");
			if (t.has(_cond)) {
				Case cse = this.parseCase(env, t.get(_cond));
				cse.setNameSuffix(name, suffix);
				return cse;
			}
			if (t.has(_where)) {
				Tree<?> where = t.get(_where);
				return new NameCase(name, suffix, where.getTag().getSymbol(), env.parseCode(env, where.get(_right)));
			}
			return new NameCase(name, suffix, "", null);
		}
		default:
			throw new ErrorCode(t, TFmt.undefined_syntax__YY0, t.getTag());
		}
	}

	public static class RuleCode extends CodeN implements CodeBuilder {
		Case iCase;

		RuleCode(Case iCase, Code bodyCode) {
			super(Ty.tAuto, iCase, bodyCode);
			this.iCase = iCase;
		}

		public boolean same(RuleCode ruleCode) {
			// TODO Auto-generated method stub
			return true;
		}

		void desugar(Code target) {
			List<Code> ands = new ArrayList<>();
			this.iCase.makeCondCode(target, ands);
			this.args[0] = this.and(ands);
			Set<String> names = new HashSet<>();
			this.findNames(this.args[1], names);
			if (names.size() > 0) {
				List<Code> vars = new ArrayList<>();
				this.iCase.declCode(names, vars);
				if (vars.size() > 0) {
					vars.add(this.args[1]);
					this.args[1] = new MultiCode(vars);
				}
			}
		}

		void findNames(Code c, Set<String> names) {
			if (c instanceof NameCode) {
				names.add(((NameCode) c).getName());
			} else {
				for (Code sub : c.args()) {
					this.findNames(sub, names);
				}
			}
		}

		@Override
		public Ty getType() {
			return this.args[1].getType();
		}

		@Override
		public Code asType(TEnv env, Ty t) {
			this.args[0] = this.args[0].asType(env, Ty.tBool);
			this.args[1] = this.args[1].asType(env, t);
			return this;
		}

		public Code condCode() {
			return this.args[0];
		}

		public Code thenCode() {
			return this.args[1];
		}

		@Override
		public void emitCode(TEnv env, TCodeSection sec) {
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("| ");
			StringCombinator.append(sb, this.iCase);
			sb.append(" => ");
			StringCombinator.append(sb, this.thenCode());
		}
	}

	static abstract class Case extends CodeN implements CodeBuilder {
		String name = null;
		String suffix = "";
		Ty nameTy = null;
		Code target = null;

		Case() {
			super(TArrays.emptyCodes);
		}

		Case(Code... values) {
			super(values);
		}

		void setNameSuffix(String name, String suffix) {
			this.name = name;
			this.suffix = suffix;
		}

		void setNameType(Ty nameTy) {
			this.nameTy = nameTy;
		}

		public abstract void makeCondCode(Code target, List<Code> ands);

		public void declCode(Set<String> names, List<Code> vars) {
			if (this.size() > 0 && this.args[0] instanceof Case) {
				for (int i = this.size() - 1; i >= 0; i--) {
					Case inner = (Case) this.args[i];
					inner.declCode(names, vars);
				}
			}
			if (this.name != null && this.target != null) {
				if (!names.contains(this.name)) {
					vars.add(new LetCode(this.name, null, this.target));
				}
			}
		}

		@Override
		public void strOut(StringBuilder sb) {
		}

		@Override
		public void emitCode(TEnv env, TCodeSection sec) {
		}

	}

	static class AnyCase extends Case {
		AnyCase() {
			super();
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("otherwise");
		}

		@Override
		public void makeCondCode(Code target, List<Code> ands) {
		}

	}

	static class NoneCase extends Case {

		NoneCase() {
			super();
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("None");
		}

		@Override
		public void makeCondCode(Code target, List<Code> ands) {
			this.target = target;
			ands.add(this.isNull(target));
		}
	}

	// case 1,2,3 => it

	static class ValuesCase extends Case {

		public ValuesCase(Code... values) {
			super(values);
		}

		@Override
		public void strOut(StringBuilder sb) {
			StringCombinator.joins(sb, this.args, ",");
		}

		@Override
		public void makeCondCode(Code target, List<Code> ands) {
			this.target = target;
			List<Code> ors = new ArrayList<>();
			Arrays.stream(this.args).forEach(c -> {
				ors.add(new BinaryCode("==", target, c));
			});
			ands.add(this.group(this.or(ors)));
		}

	}

	static class RangeCase extends Case {
		boolean inclusive;

		public RangeCase(Code start, Code end, boolean inclusive) {
			super(start, end);
			this.inclusive = inclusive;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("(");
			StringCombinator.append(sb, this.args[0]);
			sb.append(" to ");
			if (!this.inclusive) {
				sb.append("<");
			}
			StringCombinator.append(sb, this.args[1]);
			sb.append(")");
		}

		@Override
		public void makeCondCode(Code target, List<Code> ands) {
			this.target = target;
			ands.add(new BinaryCode("<=", this.args[0], target));
			ands.add(new BinaryCode(this.inclusive ? "<=" : "<", target, this.args[1]));
		}
	}

	// name
	static class NameCase extends Case {
		String op;

		public NameCase(String name, String suffix, String op, Code expr) {
			super(expr);
			this.op = op;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append(this.name + this.suffix);
		}

		@Override
		public void makeCondCode(Code target, List<Code> ands) {
			this.target = target;
			switch (this.op) {
			case "WhereEqExpr":
				ands.add(new BinaryCode("==", target, this.args[0]));
				break;
			case "WhereNeExpr":
				ands.add(new BinaryCode("!=", target, this.args[0]));
				break;
			case "WhereLtExpr":
				ands.add(new BinaryCode("<", target, this.args[0]));
				break;
			case "WhereLteExpr":
				ands.add(new BinaryCode("<=", target, this.args[0]));
				break;
			case "WhereGtExpr":
				ands.add(new BinaryCode(">", target, this.args[0]));
				break;
			case "WhereGteExpr":
				ands.add(new BinaryCode(">=", target, this.args[0]));
				break;
			case "WherePredExpr":
				ands.add(new ApplyCode(this.args[0], target));
				break;
			case "WhereNotPredExpr":
				ands.add(this.not(new ApplyCode(this.args[0], target)));
				break;
			default:
				throw new ErrorCode(TFmt.undefined_syntax__YY0, this.op);
			}
		}
	}

	// {a:T,b:T,c:T}
	static class DataCase extends Case {
		public DataCase(Case[] list) {
			super(list);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("{");
			StringCombinator.joins(sb, this.args, ", ");
			sb.append("}");
		}

		@Override
		public void makeCondCode(Code target, List<Code> ands) {
			this.target = target;
			for (Code c : this.args) {
				Case inner = (Case) c;
				String name = inner.name;
				Ty nameTy = inner.nameTy;
				ands.add(new ExistFieldCode(target, name));
				Code field = new GetCode(target, name, nameTy);
				if (nameTy.isOption()) {
					ands.add(this.isSome(field));
					inner.makeCondCode(this.get(field), ands);
				} else {
					inner.makeCondCode(field, ands);
				}
			}
		}

	}

	// [X,Y,Z]
	static class ListCase extends Case {

		public ListCase(Case[] list) {
			super(list);
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("[");
			StringCombinator.joins(sb, this.args, ", ");
			sb.append("]");
		}

		@Override
		public void makeCondCode(Code target, List<Code> ands) {
			// ArrayList<Code> l = new ArrayList<>();
			// for (Code c : this.args) {
			// ICase inner = (ICase) c;
			// String name = inner.getName();
			// l.add(new ExistFieldCode(target, name));
			// l.add(inner.condCode(new GetCode(target, name)));
			// }
			// return this.and(l);
		}

	}

}
