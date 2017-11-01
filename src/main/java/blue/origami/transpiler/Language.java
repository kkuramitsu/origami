package blue.origami.transpiler;

import java.util.Arrays;
import java.util.List;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.OFactory;
import blue.origami.common.OOption;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.AssignCode;
import blue.origami.transpiler.code.BinaryCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.VarNameCode;
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
		return "chibi";
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
		// env.add("a", VarDomain.var(0));
		// env.add("Data", Ty.tData());
		env.addNameDecl(env, "i,j,k,m,n", Ty.tInt);
		env.addNameDecl(env, "x,y,z,w", Ty.tFloat);
		env.addNameDecl(env, "s,t,u,name", Ty.tString);
	}

	/* literal */

	/* name */

	public Code typeName(Env env, VarNameCode code, Ty ret) {
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

	private Code parseNames(Env env, VarNameCode code, String name, Ty ret) {
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

	public Code typeLet(Env env, LetCode code, Ty ret) {
		if (code.isUntyped()) {
			FuncEnv fenv = env.getFuncEnv();
			if (code.isImplicit) {

			}
			Variable var = fenv.newVariable(code.getSource(), code.index, code.declType);
			env.add(code.name, var);
			code.index = var.getIndex();

			code.inner = code.inner.bindAs(env, code.declType);
			ODebug.trace("let %s %s %s", code.name, code.declType, code.inner.getType());
			code.setType(Ty.tVoid);
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
			FuncTy funcType = (FuncTy) firstType.base();
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

	public Code typeExpr(Env env, ExprCode code, Ty ret) {
		if (code.isUntyped()) {
			List<CodeMap> founds = env.findCodeMaps(code.name, code.args.length);
			this.typeArgs(env, code, founds);
			Ty[] p = Arrays.stream(code.args).map(c -> c.getType()).toArray(Ty[]::new);
			// ODebug.trace("founds=%s", founds);
			if (founds.size() == 0) {
				return code.asUnfound(env, founds).castType(env, ret);
			}
			if (founds.size() == 1) {
				return this.typeMatchedExpr(env, code, founds.get(0).generate(env, p), ret);
			}
			// code.typeArgs(env, founds);
			CodeMap selected = CodeMap.select(env, founds, ret, p, code.maxCost());
			if (selected == null) {
				return code.asMismatched(env, founds).castType(env, ret);
			}
			return this.typeMatchedExpr(env, code, selected.generate(env, p), ret);
		}
		return code.castType(env, ret);
	}

	private void typeArgs(Env env, ExprCode code, List<CodeMap> l) {
		for (int i = 0; i < code.args.length; i++) {
			Ty pt = this.getCommonParamType(l, i);
			// ODebug.trace("common[%d] %s", i, pt);
			code.args[i] = code.args[i].asType(env, pt);
			// ODebug.trace("typed[%d] %s %s", i, this.args[i],
			// this.args[i].getType());
		}
	}

	private Ty getCommonParamType(List<CodeMap> l, int n) {
		// Ty ty = l.get(0).getParamTypes()[n];
		// ODebug.trace("DD %s", l);
		// for (int i = 1; i < l.size(); i++) {
		// if (!ty.eq(l.get(i).getParamTypes()[n])) {
		return Ty.tUntyped();
		// }
		// }
		// return ty;
	}

	private Code typeMatchedExpr(Env env, ExprCode code, CodeMap found, Ty t) {
		Ty[] dpars = found.getParamTypes();
		Ty dret = found.getReturnType();
		if (found.isGeneric()) {
			VarDomain dom = new VarDomain(dpars);
			Ty[] gpars = dom.conv(dpars);
			Ty gret = dom.conv(dret);
			for (int i = 0; i < code.args.length; i++) {
				code.args[i] = code.args[i].asType(env, gpars[i]);
				// if (!found.isAbstract()) {
				// if (dpars[i] instanceof VarParamTy) {
				// ODebug.trace("MUST upcast %s => %s", gpars[i], gpars[i]);
				// code.args[i] = new BoxCastCode(gpars[i], code.args[i]);
				// }
				// if (dpars[i] instanceof FuncTy && dpars[i].hasSome(Ty.IsGeneric)) {
				// VarDomain dom2 = new VarDomain(dpars);
				// dom2.useParamVar();
				// Ty anyTy = dpars[i].dupVar(dom2).memoed(); // AnyRef
				// CodeMap conv = env.findTypeMap(env, gpars[i], anyTy);
				// ODebug.trace("MUST funccast %s => %s :: %s", gpars[i], anyTy, conv);
				// code.args[i] = new FuncCastCode(anyTy, conv, code.args[i]);
				// }
				// }
			}
			if (found.isMutation()) {
				Ty ty = code.args[0].getType();
				ODebug.trace("MUTATION %s", ty);
				ty.foundMutation();
			}
			code.setMapped(found);
			code.setType(gret);
			Code result = code;
			// if (!found.isAbstract()) {
			// if (found.getReturnType() instanceof VarParamTy) {
			// ODebug.trace("must downcast %s => %s", found.getReturnType(), gret);
			// result = new UnboxCastCode(gret, result);
			// }
			// if (found.getReturnType() instanceof FuncTy &&
			// found.getReturnType().hasSome(Ty.IsGeneric)) {
			// VarDomain dom2 = new VarDomain(dpars);
			// dom2.useParamVar();
			// Ty anyTy = found.getReturnType().dupVar(dom2); // AnyRef
			// CodeMap conv = env.findTypeMap(env, gret, anyTy);
			// ODebug.trace("MUST funccast %s => %s :: %s", anyTy, gret, conv);
			// result = new FuncCastCode(gret, conv, result);
			// }
			// }
			return result.castType(env, t);
		} else {
			for (int i = 0; i < code.args.length; i++) {
				code.args[i] = code.args[i].asType(env, dpars[i]);
			}
			if (found.isMutation()) {
				Ty ty = code.args[0].getType();
				ODebug.trace("MUTATION %s", ty);
				ty.foundMutation();
			}
			code.setMapped(found);
			code.setType(dret);
			return code.castType(env, t);
		}
	}
}
