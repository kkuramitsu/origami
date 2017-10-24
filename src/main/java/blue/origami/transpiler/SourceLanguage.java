package blue.origami.transpiler;

import blue.origami.common.OFactory;
import blue.origami.common.OOption;
import blue.origami.transpiler.rule.BinaryExpr;
import blue.origami.transpiler.rule.DataExpr;
import blue.origami.transpiler.rule.DataType;
import blue.origami.transpiler.rule.DictExpr;
import blue.origami.transpiler.rule.ListExpr;
import blue.origami.transpiler.rule.RangeExpr;
import blue.origami.transpiler.rule.SourceUnit;
import blue.origami.transpiler.rule.UnaryExpr;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;

public class SourceLanguage implements OFactory<SourceLanguage> {

	@Override
	public Class<?> keyClass() {
		return SourceLanguage.class;
	}

	@Override
	public SourceLanguage clone() {
		return this.newClone();
	}

	@Override
	public void init(OOption options) {

	}

	public void init(Env env) {
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

}
