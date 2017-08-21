package blue.origami.transpiler;

import java.util.Arrays;

import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.SimpleTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMap;

public class SourceType extends TypeMap<String> {

	SourceSection head;

	public SourceType(TEnv env) {
		super(env);
	}

	public void setTypeDeclSection(SourceSection head) {
		this.head = head;
	}

	@Override
	public String type(Ty ty) {
		if (!this.isDyLang) {
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
		return this.env.getSymbolOrElse(name, name);
	}

	@Override
	protected String mapDefaultType(String prefix, Ty ty, String inner) {
		String p = prefix.replace("'", "");
		String[] keys = { prefix + inner, prefix + "[a]", p + inner, p + "[a]", prefix };
		return String.format(this.env.getSymbol(keys), inner);
	}

	@Override
	protected String key(FuncTy funcTy) {
		return "F" + funcTy.toString();
	}

	@Override
	protected String gen(FuncTy funcTy) {
		String funcdef = this.env.getSymbolOrElse("functypedef", null);
		if (funcdef != null) {
			String typeName = "F" + this.seq() + this.comment(funcTy.toString());
			Param p = new Param(funcTy.getParamTypes());
			this.head.pushf(this.env, funcdef, funcTy.getReturnType(), typeName, p);
			return typeName;
		}
		return funcTy.toString();
	}

	@Override
	protected String key(DataTy dataTy) {
		return dataTy.names().toString();
	}

	@Override
	protected String gen(DataTy dataTy) {
		return dataTy.toString();
	}

	private String commentFmt = null;

	String comment(String s) {
		if (this.commentFmt == null) {
			this.commentFmt = this.env.getSymbolOrElse("comment", "");
		}
		return String.format(this.commentFmt, s);
	}

	Ty box(Ty ty) {
		String key = ty.key() + "^";
		String t = this.env.getSymbolOrElse(key, null);
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

	String genFieldClass(TEnv env, String cname, String name) {
		String key = "F$" + name;
		if (!this.typeMap.containsKey(key)) {
			NameHint hint = env.findGlobalNameHint(env, name);
			this.head.pushf(env, "fieldtype", "", cname, hint.getType(), name);
			this.typeMap.put(key, key);
		}
		return key;
	}

}