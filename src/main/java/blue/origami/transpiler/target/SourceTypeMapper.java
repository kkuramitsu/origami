package blue.origami.transpiler.target;

import java.util.Arrays;

import blue.origami.common.ODebug;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.SimpleTy;
import blue.origami.transpiler.type.TupleTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMapper;

public class SourceTypeMapper extends TypeMapper<String> {

	SyntaxMapper syntax;
	SourceSection head;

	public SourceTypeMapper(Transpiler env) {
		super(env);
	}

	public void setSyntaxMapper(SyntaxMapper syntax) {
		this.syntax = syntax;
	}

	public void setTypeSection(SourceSection head) {
		this.head = head;
	}

	@Override
	public String type(Ty ty) {
		if (!this.syntax.isDyLang) {
			return ty.mapType(this);
		}
		return "";
	}

	@Override
	public String[] types(Ty... ty) {
		return Arrays.stream(ty).map(t -> this.type(t)).toArray(String[]::new);
	}

	@Override
	public String key(String c) {
		return "[" + c + "]";
	}

	@Override
	protected String mapDefaultType(String name) {
		return this.syntax.symbol(name, name);
	}

	@Override
	protected String mapDefaultType(String prefix, Ty ty, String inner) {
		String p = prefix.replace("'", "");
		String[] keys = { prefix + inner, prefix + "[a]", p + inner, p + "[a]", prefix };
		return String.format(this.syntax.fmt(keys), inner);
	}

	@Override
	protected String keyFuncType(FuncTy funcTy) {
		return "F" + funcTy.toString();
	}

	@Override
	protected String genFuncType(FuncTy funcTy) {
		String funcdef = this.syntax.symbol("functypedef", (String) null);
		if (funcdef != null) {
			String typeName = "F" + this.seq() + this.comment(funcTy.toString());
			SourceParamCode p = new SourceParamCode(this.syntax, funcTy.getParamTypes());
			this.head.pushf(funcdef, funcTy.getReturnType(), typeName, p);
			return typeName;
		}
		return funcTy.toString();
	}

	@Override
	protected String keyTupleType(TupleTy tupleTy) {
		return "T" + tupleTy.toString();
	}

	@Override
	protected String genTupleType(TupleTy tupleTy) {
		ODebug.TODO();
		return tupleTy.toString();
	}

	@Override
	protected String keyDataType(DataTy dataTy) {
		return dataTy.names().toString();
	}

	@Override
	protected String genDataType(DataTy dataTy) {
		return dataTy.toString();
	}

	private String commentFmt = null;

	String comment(String s) {
		if (this.commentFmt == null) {
			this.commentFmt = this.syntax.symbol("comment", "");
		}
		return String.format(this.commentFmt, s);
	}

	Ty box(Ty ty) {
		String key = ty.toString() + "^";
		String t = this.syntax.symbol(key, (String) null);
		if (t != null) {
			return new SimpleTy(key);
		}
		return ty;
	}

	// String genDataType(TEnv env, DataTy dataTy) {
	// Set<String> names = dataTy.names();
	// String key = names.toString();
	// String typeName = this.typeMap.get(key);
	// if (typeName == null) {
	// final String cname = "D$" + this.typeMap.size();
	// String[] inf = names.stream().map(f -> this.genFieldClass(env, cname,
	// f)).toArray(String[]::new);
	// this.head.pushf(env, "class implements", "class %1$s implements %2$s {",
	// cname, inf);
	// for (String name : names) {
	// NameHint hint = env.findGlobalNameHint(env, name);
	// this.head.pushf(env, "class field", "\t%2$s = %3$s;\n", cname,
	// hint.getType(), name);
	// }
	// this.head.pushf("end class", "class");
	// this.typeMap.put(key, cname);
	// return cname;
	// }
	// return typeName;
	// }

	String genFieldClass(Env env, String cname, String name) {
		String key = "F$" + name;
		if (!this.typeMap.containsKey(key)) {
			NameHint hint = env.findGlobalNameHint(env, name);
			this.head.pushf("fieldtype", "", cname, hint.getType(), name);
			this.typeMap.put(key, key);
		}
		return key;
	}

}
