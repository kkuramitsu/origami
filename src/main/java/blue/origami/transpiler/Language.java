package blue.origami.transpiler;

import blue.origami.common.OArrays;
import blue.origami.common.OFactory;
import blue.origami.common.OOption;
import blue.origami.transpiler.FunctionContext.Variable;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.AssignCode;
import blue.origami.transpiler.code.BinaryCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.rule.BinaryExpr;
import blue.origami.transpiler.rule.DataExpr;
import blue.origami.transpiler.rule.DataType;
import blue.origami.transpiler.rule.DictExpr;
import blue.origami.transpiler.rule.ListExpr;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.rule.RangeExpr;
import blue.origami.transpiler.rule.SourceUnit;
import blue.origami.transpiler.rule.UnaryExpr;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarLogger;

public class Language implements OFactory<Language> {

	@Override
	public Class<?> keyClass() {
		return Language.class;
	}

	@Override
	public Language clone() {
		return this.newClone();
	}

	@Override
	public void init(OOption options) {

	}

	public String getLangName() {
		return "konoha5";
	}

	public void initMe(Env env) {
		env.add("Source", new SourceUnit());
		env.add("AddExpr", new BinaryExpr("+"));
		env.add("SubExpr", new BinaryExpr("-"));
		env.add("CatExpr", new BinaryExpr("++"));
		env.add("PowExpr", new BinaryExpr("^"));
		env.add("MulExpr", new BinaryExpr("*"));
		env.add("DivExpr", new BinaryExpr("/"));
		env.add("ModExpr", new BinaryExpr("%"));
		env.add("EqExpr", new BinaryExpr("=="));
		env.add("NeExpr", new BinaryExpr("!="));
		env.add("LtExpr", new BinaryExpr("<"));
		env.add("LteExpr", new BinaryExpr("<="));
		env.add("GtExpr", new BinaryExpr(">"));
		env.add("GteExpr", new BinaryExpr(">="));
		env.add("LAndExpr", new BinaryExpr("&&"));
		env.add("LOrExpr", new BinaryExpr("||"));
		env.add("AndExpr", new BinaryExpr("&&"));
		env.add("OrExpr", new BinaryExpr("||"));
		env.add("XorExpr", new BinaryExpr("^^"));
		env.add("LShiftExpr", new BinaryExpr("<<"));
		env.add("RShiftExpr", new BinaryExpr(">>"));

		env.add("BindExpr", new BinaryExpr("flatMap"));
		env.add("ConsExpr", new BinaryExpr("cons"));
		env.add("OrElseExpr", new BinaryExpr("!?"));

		env.add("NotExpr", new UnaryExpr("!"));
		env.add("MinusExpr", new UnaryExpr("-"));
		env.add("PlusExpr", new UnaryExpr("+"));
		env.add("CmplExpr", new UnaryExpr("~"));

		env.add("DataListExpr", new ListExpr(true));
		env.add("RangeUntilExpr", new RangeExpr(false));
		env.add("DataDictExpr", new DictExpr(true));
		env.add("RecordExpr", new DataExpr(false));

		env.add("RecordType", new DataType(false));
		env.add("DataType", new DataType(true));

		// type
		// env.add("?", Ty.tUntyped0);
		env.add("Bool", Ty.tBool);
		env.add("Int", Ty.tInt);
		env.add("Float", Ty.tFloat);
		env.add("String", Ty.tString);
		env.add("a", VarDomain.var(0));
		env.add("b", VarDomain.var(1));
		env.add("c", VarDomain.var(2));
		env.add("d", VarDomain.var(3));
		env.add("e", VarDomain.var(4));
		env.add("f", VarDomain.var(5));
		// env.add("Data", Ty.tData());
		env.addNameDecl(env, "i,j,k,m,n", Ty.tInt);
		env.addNameDecl(env, "x,y,z,w", Ty.tFloat);
		env.addNameDecl(env, "s,t,u,name", Ty.tString);
	}

	/* literal */

	/* name */

	public Code typeName(Env env, NameCode code, Ty ret) {
		if (code.isUntyped()) {
			NameInfo ref = env.get(code.name, NameInfo.class, (e, c) -> e.isNameInfo(env) ? e : null);
			if (ref != null) {
				ref.used(env);
				return ref.newNameCode(env, code.getSource()).castType(env, ret);
			}
			return this.parseNames(env, code, code.name, ret);
		}
		return code.castType(env, ret);
	}

	private Code parseNames(Env env, NameCode code, String name, Ty ret) {
		Code mul = null;
		for (int i = 0; i < name.length(); i++) {
			String var = this.parseName(name, i);
			NameInfo ref = env.get(var, NameInfo.class, (e, c) -> e.isNameInfo(env) ? e : null);
			if (ref == null) {
				NameHint hint = env.findNameHint(env, name);
				if (hint != null) {
					return new ErrorCode(code, TFmt.undefined_name__YY1__YY2, code.name, hint.getType());
				}
				return new ErrorCode(code, TFmt.undefined_name__YY1, code.name);
			}
			ref.used(env);
			mul = this.mul(mul, ref.newNameCode(env, code.getSource()));
		}
		return mul.asType(env, ret);
	}

	private String parseName(String name, int index) {
		int end = index + 1;
		while (end < name.length()) {
			char c = name.charAt(end);
			if (Character.isDigit(c) || c == '\'') {
				end++;
			} else {
				break;
			}
		}
		return name.substring(index, end);
	}

	private Code mul(Code left, Code right) {
		if (left == null) {
			return right;
		}
		return new BinaryCode("*", left, right);
	}

	public Code typeAssign(Env env, AssignCode code, Ty ret) {
		if (code.isUntyped()) {
			Variable ref = env.get(code.name, Variable.class, (e, c) -> e.isNameInfo(env) ? e : null);
			if (ref != null && ref.getLevel() == 0) {
				ref.used(env);
				code.index = ref.getIndex();
				code.inner = code.inner.bindAs(env, ref.getType());
				code.setType(ref.getType());
			} else {
				NameInfo ref2 = env.get(code.name, NameInfo.class, (e, c) -> e.isNameInfo(env) ? e : null);
				if (ref2 == null) {
					return new ErrorCode(code, TFmt.undefined_name__YY1, code.name);
				} else {
					return new ErrorCode(code, TFmt.immutable_name__YY1, code.name);
				}
			}
		}
		if (ret.isVoid()) {
			code.setType(ret);
		}
		return code;
	}

	public Code typeApply(Env env, ApplyCode code, Ty ret) {
		if (!code.isUntyped()) {
			return code.castType(env, ret);
		}
		code.args[0] = code.args[0].asType(env, Ty.tUntyped());
		if (code.args[0] instanceof FuncRefCode) {
			// ODebug.trace("switching to expr %s", code.args[0]);
			String name = ((FuncRefCode) code.args[0]).getName();
			return new ExprCode(name, OArrays.ltrim(code.args)).asType(env, ret);
		}

		Ty firstType = code.args[0].getType();
		if (firstType.isFunc()) {
			FuncTy funcType = (FuncTy) firstType.real();
			Ty[] p = funcType.getParamTypes();
			if (p.length + 1 != code.args.length) {
				throw new ErrorCode(TFmt.mismatched_parameter_size_S_S, p.length, code.args.length);
			}
			for (int i = 0; i < p.length; i++) {
				code.args[i + 1] = code.args[i + 1].asType(env, p[i]);
			}
			code.setType(funcType.getReturnType());
			return code.castType(env, ret);
		}
		if (firstType.isVar()) {
			Ty[] p = new Ty[code.args.length - 1];
			for (int i = 1; i < code.args.length; i++) {
				code.args[i] = code.args[i].asType(env, Ty.tUntyped());
				p[i - 1] = code.args[i].getType();
			}
			Ty funcType = Ty.tFunc(ret, p);
			firstType.acceptTy(Code.bSUB, funcType, VarLogger.Update);
			code.setType(ret);
			return code;
		}
		throw new ErrorCode(code.args[0], TFmt.not_function__YY1, code.args[0].getType());
	}

}
