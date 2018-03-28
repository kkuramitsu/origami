package blue.origami.transpiler.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.FuncEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.Ty;
import origami.libnez.OStrings;

public class MatchCode extends CodeN implements CodeBuilder {

	Code targetCode;
	RuleCode optionalCase = null;

	public MatchCode(Code targetCode, RuleCode optionalCase, RuleCode... cases) {
		super(Ty.tAuto, cases);
		this.targetCode = targetCode;
		this.optionalCase = optionalCase;
	}

	@Override
	public Ty getType() {
		return this.args[0].getType();
	}

	public Ty inferTargetType() {
		Ty infTy = null;
		for (Code c : this.args) {
			RuleCode r = (RuleCode) c;
			Ty ty = r.inferType();
			// ODebug.trace(":::::: %s %s", c, ty);
			if (ty != null) {
				infTy = ty;
				if (!infTy.hasSome(Ty.IsVar)) {
					break;
				}
			}
		}
		if (infTy == null) {
			infTy = Ty.tVar(null);
		}
		return infTy;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.targetCode == null) {
			FuncEnv fenv = env.getFuncEnv();
			if (fenv.getParamSize() == 0) {
				throw new ErrorCode(TFmt.required_first_argument);
			}
			this.targetCode = fenv.getArgumentsPattern(env);
		}
		Ty targetTy = this.inferTargetType();
		Code desugar = null;
		if (this.optionalCase != null) {
			this.targetCode = this.targetCode.asType(env, Ty.tOption(targetTy));
			LetCode decl = new LetCode("it", this.targetCode);
			this.targetCode = new VarNameCode("it");
			Code ifCode = new IfCode(this.isNone(this.targetCode), this.optionalCase.thenCode(),
					this.desugarRule(env, this.getSome(this.targetCode), targetTy));
			desugar = decl.add(ifCode);
		} else {
			desugar = this.desugarRule(env, this.targetCode, targetTy);
		}
		ODebug.trace("match %s", desugar);
		return desugar.asType(env, ret);
	}

	private Code desugarRule(Env env, Code targetCode0, Ty targetTy) {
		Code targetCode = targetCode0.asType(env, targetTy);
		targetTy = targetCode.getType();
		LetCode decl = new LetCode("it", targetCode);
		targetCode = new VarNameCode("it");
		for (Code c : this.args) {
			RuleCode r = (RuleCode) c;
			r.desugar0(targetCode, targetTy);
			ODebug.trace("desugared %s", r);
		}
		return this.toIfCode(decl);
	}

	private Code toIfCode(Code decl) {
		int last = this.size() - 1;
		RuleCode r = this.get(last);
		Code elseCode = r.thenCode();
		for (int i = last - 1; i >= 0; i--) {
			r = this.get(i);
			elseCode = new IfCode(r.condCode(), r.thenCode(), elseCode);
		}
		return (decl == null) ? elseCode : decl.add(elseCode);
	}

	private RuleCode get(int index) {
		return (RuleCode) this.args[index];
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("match (");
		OStrings.append(sb, this.targetCode);
		sb.append(") ");
		Arrays.stream(this.args).forEach((r) -> {
			r.strOut(sb);
		});
	}

	@Override
	public void emitCode(CodeSection sec) {

	}

	public static class RuleCode extends CodeN implements CodeBuilder {
		Case caseCode;

		public RuleCode(Case caseCode, Code bodyCode) {
			super(Ty.tAuto, caseCode, bodyCode);
			this.caseCode = caseCode;
		}

		public boolean match(RuleCode prev) {
			return this.caseCode.match(prev.caseCode);
		}

		public Ty inferType() {
			return this.caseCode.inferType();
		}

		void desugar0(Code target, Ty targetTy) {
			List<Code> ands = new ArrayList<>();
			this.caseCode.makeCondCode(target, targetTy, ands);
			this.args[0] = this.and(ands);
			Set<String> names = new HashSet<>();
			this.findNames(this.args[1], names);
			ODebug.trace("names %s", names);
			if (names.size() > 0) {
				List<Code> vars = new ArrayList<>();
				this.caseCode.declCode(names, vars);
				if (vars.size() > 0) {
					vars.add(this.args[1]);
					this.args[1] = new MultiCode(vars);
				}
			}
		}

		void findNames(Code c, Set<String> names) {
			if (c instanceof VarNameCode) {
				names.add(((VarNameCode) c).getName());
				return;
			}
			for (Code sub : c.args()) {
				this.findNames(sub, names);
			}
		}

		@Override
		public Ty getType() {
			return this.args[1].getType();
		}

		@Override
		public Code asType(Env env, Ty t) {
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
		public void emitCode(CodeSection sec) {
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("| ");
			OStrings.append(sb, this.caseCode);
			if (this.caseCode != this.condCode()) {
				sb.append(" if ");
				OStrings.append(sb, this.condCode());
			}
			sb.append(" => ");
			OStrings.append(sb, this.thenCode());
		}
	}

	public static abstract class Case extends CodeN implements CodeBuilder {
		String name = null;
		String suffix = "";
		Code target = null;
		Ty targetTy = null;

		Case() {
			super(OArrays.emptyCodes);
		}

		Case(Code... values) {
			super(values);
		}

		public boolean match(Case prev) {
			return !(prev instanceof ListCase || prev instanceof DataCase);
		}

		public String getName() {
			return this.name;
		}

		public void setNameSuffix(String name, String suffix) {
			this.name = name;
			this.suffix = suffix;
		}

		public boolean isZeroMore() {
			return Objects.equals("*", this.suffix);
		}

		public void makeCondCode(Code target, Ty ty, List<Code> ands) {
			this.target = target;
			if (this.targetTy == null) {
				this.targetTy = ty;
			}
			this.extractCondCode(ands);
		}

		public abstract Ty inferType();

		public abstract void extractCondCode(List<Code> ands);

		public void declCode(Set<String> names, List<Code> vars) {
			if (this.size() > 0 && this.args[0] instanceof Case) {
				for (int i = this.size() - 1; i >= 0; i--) {
					Case inner = (Case) this.args[i];
					inner.declCode(names, vars);
				}
			}
			if (this.name != null && this.target != null) {
				if (names.contains(this.name)) {
					vars.add(new LetCode(AST.getName(this.name), this.targetTy, this.target));
				}
			}
		}

		@Override
		public void emitCode(CodeSection sec) {
		}

	}

	public static class AnyCase extends Case {
		public AnyCase() {
			super();
		}

		@Override
		public boolean match(Case prev) {
			return true;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("otherwise");
		}

		@Override
		public Ty inferType() {
			return null;
		}

		@Override
		public void extractCondCode(List<Code> ands) {
		}

	}

	public static class NoneCase extends Case {

		public NoneCase() {
			super();
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("None");
		}

		@Override
		public Ty inferType() {
			return null;
		}

		@Override
		public void extractCondCode(List<Code> ands) {
			ands.add(this.isNone(this.target));
		}
	}

	// case 1,2,3 => it

	public static class ValuesCase extends Case {

		public ValuesCase(Code... values) {
			super(values);
		}

		@Override
		public void strOut(StringBuilder sb) {
			OStrings.joins(sb, this.args, ",");
		}

		@Override
		public Ty inferType() {
			for (Code c : this.args) {
				Ty ty = c.getType();
				if (ty != null) {
					return ty;
				}
			}
			return null;
		}

		@Override
		public void extractCondCode(List<Code> ands) {
			List<Code> ors = new ArrayList<>();
			Arrays.stream(this.args).forEach(c -> {
				ors.add(new BinaryCode("==", this.target, c));
			});
			ands.add(this.group(this.or(ors)));
		}
	}

	public static class RangeCase extends Case {
		boolean inclusive;

		public RangeCase(Code start, Code end, boolean inclusive) {
			super(start, end);
			this.inclusive = inclusive;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("(");
			OStrings.append(sb, this.args[0]);
			sb.append(" to ");
			if (!this.inclusive) {
				sb.append("<");
			}
			OStrings.append(sb, this.args[1]);
			sb.append(")");
		}

		@Override
		public Ty inferType() {
			for (Code c : this.args) {
				Ty ty = c.getType();
				if (ty != null) {
					return ty;
				}
			}
			return null;
		}

		@Override
		public void extractCondCode(List<Code> ands) {
			ands.add(this.op(this.args[0], "<=", this.target));
			ands.add(this.op(this.target, this.inclusive ? "<=" : "<", this.args[1]));
		}
	}

	// name
	public static class NameCase extends Case {
		String op;

		public NameCase(String name, String suffix, String op, Code expr) {
			super(expr);
			this.name = name;
			this.suffix = suffix;
			this.op = op;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append(this.name + this.suffix);
		}

		@Override
		public Ty inferType() {
			return null;
		}

		@Override
		public void extractCondCode(List<Code> ands) {
			switch (this.op) {
			case "":
				break;
			case "WhereEqExpr":
				ands.add(this.op(this.target, "==", this.args[0]));
				break;
			case "WhereNeExpr":
				ands.add(this.op(this.target, "!=", this.args[0]));
				break;
			case "WhereLtExpr":
				ands.add(this.op(this.target, "<", this.args[0]));
				break;
			case "WhereLteExpr":
				ands.add(this.op(this.target, "<=", this.args[0]));
				break;
			case "WhereGtExpr":
				ands.add(this.op(this.target, ">", this.args[0]));
				break;
			case "WhereGteExpr":
				ands.add(this.op(this.target, ">=", this.args[0]));
				break;
			case "WherePredExpr":
				ands.add(new ApplyCode(this.args[0], this.target));
				break;
			case "WhereNotPredExpr":
				ands.add(this.not(new ApplyCode(this.args[0], this.target)));
				break;
			default:
				throw new ErrorCode(TFmt.undefined_syntax__YY1, this.op);
			}
		}

	}

	// {a:T,b:T,c:T}
	public static class DataCase extends Case {
		public DataCase(Case[] list) {
			super(list);
		}

		@Override
		public boolean match(Case prev) {
			return prev instanceof DataCase;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("{");
			OStrings.joins(sb, this.args, ",");
			sb.append("}");
		}

		@Override
		public Ty inferType() {
			return Ty.tData();
		}

		@Override
		public void extractCondCode(List<Code> ands) {
			for (Code c : this.args) {
				Case inner = (Case) c;
				String name = inner.getName();
				Ty nameTy = inner.targetTy;
				ands.add(new HasCode(this.target, name));
				Code field = new GetCode(this.target, name, nameTy);
				if (nameTy.isGeneric(Ty.tOption)) {
					ands.add(this.isSome(field));
					inner.makeCondCode(field, nameTy.getParamType(), ands);
				} else {
					inner.makeCondCode(field, nameTy, ands);
				}
			}
		}
	}

	// [X,Y,Z]
	public static class TupleCase extends Case {

		public TupleCase(Case[] list) {
			super(list);
			assert (list.length > 1) : "tuple " + 1;
		}

		@Override
		public boolean match(Case prev) {
			return prev instanceof TupleCase;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("(");
			OStrings.joins(sb, this.args, ",");
			sb.append(")");
		}

		private Case caseAt(int index) {
			return (Case) this.args[index];
		}

		@Override
		public Ty inferType() {
			Ty[] ts = new Ty[this.args.length];
			for (int i = 0; i < this.args.length; i++) {
				ts[i] = Ty.tVar(null);
			}
			return Ty.tTuple(ts);
		}

		@Override
		public void extractCondCode(List<Code> ands) {
			int len = this.args.length;
			for (int i = 0; i < len; i++) {
				Case c = this.caseAt(i);
				c.makeCondCode(this.tupleAt(this.target, i), this.targetTy.getParamType(), ands);
			}
		}

	}

	// [X,Y,Z]
	public static class ListCase extends Case {

		public ListCase(Case[] list) {
			super(list);
		}

		@Override
		public boolean match(Case prev) {
			return prev instanceof ListCase;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("[");
			OStrings.joins(sb, this.args, ",");
			sb.append("]");
		}

		private Case caseAt(int index) {
			return (Case) this.args[index];
		}

		private boolean isTailZeroMore() {
			if (this.args.length > 1) {
				return this.caseAt(this.args.length - 1).isZeroMore();
			}
			return false;
		}

		@Override
		public Ty inferType() {
			// for (Code c : this.args) {
			// Ty ty = ((Case) c).inferType();
			// if (ty != null) {
			// return Ty.tList(ty);
			// }
			// }
			return Ty.tList(Ty.tVar(null));
		}

		@Override
		public void extractCondCode(List<Code> ands) {
			int len = this.args.length;
			if (this.isTailZeroMore()) {
				ands.add(this.op(this.len(this.target), ">=", len - 1));
				for (int i = 0; i < len - 1; i++) {
					Case c = this.caseAt(i);
					c.makeCondCode(this.geti(this.target, i), this.targetTy.getParamType(), ands);
				}
				this.caseAt(len - 1).makeCondCode(this.tail(this.target, len - 1), this.targetTy, ands);
				return;
			}
			ands.add(this.op(this.len(this.target), "==", len));
			for (int i = 0; i < len; i++) {
				Case c = this.caseAt(i);
				c.makeCondCode(this.geti(this.target, i), this.targetTy.getParamType(), ands);
			}
		}

	}

}
