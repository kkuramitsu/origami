package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.VarTy;
import blue.origami.transpiler.type.Ty;

public class DataCode extends CodeN {
	protected String[] names;
	boolean isMutable = false;
	private String cnt = "";

	public DataCode(boolean isMutable, String[] names, Code[] values) {
		super(values);
		this.names = names;
		this.isMutable = isMutable;
	}

	public DataCode(Ty dt) { // DefaultValue
		super(dt, OArrays.emptyCodes);
		this.names = OArrays.emptyNames;
		this.isMutable = dt.isMutable();
	}

	public String[] getNames() {
		return this.names;
	}

	public boolean isMutable() {
		return this.isMutable;
	}

	public String getCnt() {
		return this.cnt;
	}

	public Code cast(Env env, Ty ret) {
		if (ret.isVar()) {
      return cast(env, Ty.tData(((VarTy)ret).getName()));
    }else if (!(ret.isData())) {
      throw new ErrorCode(this, TFmt.type_error_YY1_YY2, ret, "Data");
    }
    DataTy dt = (DataTy) ret;
    String[] retNames = dt.names();
		if (retNames.length != this.names.length) {
			throw new ErrorCode(this, TFmt.failed_type_inference);
		}
    for (int i = 0; i < this.names.length; i++) {
			if (retNames[i].equals(this.names[i])) {
				continue;
			}
      NameHint retHint = env.findGlobalNameHint(env, retNames[i]);
      if (retHint != null) {
        Ty retTy = retHint.getType().base();
				if (retTy == env.findGlobalNameHint(env, this.names[i]).getType().base()) {
					continue;
				}
				this.args[i] = this.args[i].asType(env, retTy);
      } else {
        throw new ErrorCode(TFmt.undefined_name__YY1, retNames[i]);
      }
    }
		return this;
	}

	private Code addNameHint(Env env, String key, Code value) {
		NameHint hint = env.findGlobalNameHint(env, key);
		if (hint != null) {
			Ty ty = hint.getType();
			if (!hint.equalsName(key) && hint.isLocalOnly()) {
				env.addGlobalName(env, key, ty);
			} else {
				hint.useGlobal();
			}
			return value.asType(env, ty);
		} else {
			Ty ty = Ty.tUntyped();
			Code newValue = value.asType(env, ty);
			Ty newTy = newValue.getType();
			if (ty == newTy) {
				throw new ErrorCode(value, TFmt.failed_type_inference);
			}
			ODebug.trace("implicit name definition %s as %s", key, newTy);
			env.addGlobalName(env, key, newTy);
			return newValue;
		}
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			int id = env.getTranspiler().getCnt();
			DataTy dt = Ty.tData(id, this.names);
			this.setType(dt);

			this.cnt = DataTy.makeCnt(id);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = addNameHint(env, this.names[i], this.args[i]);
				this.names[i] = this.names[i] + this.cnt;
				addNameHint(env, this.names[i], this.args[i]);
			}
		}
		return super.castType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushData(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		//this.sexpr(sb, this.isMutable() ? "data" : "record", 0, this.names.length, (n) -> {
		this.sexpr(sb, this.isMutable() ? Ty.Mut + "data" : "data", 0, this.names.length, (n) -> {
			sb.append(DataTy.deleteCnt(this.names[n]));
			sb.append(":");
			OStrings.append(sb, this.args[n]);
		});
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		//sh.Token(this.isMutable() ? "{" : "[");
		sh.Token(this.isMutable() ? Ty.Mut + "{" : "{");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sh.Token(",");
			}
			sh.Name(DataTy.deleteCnt(this.names[i]));
			sh.Token(":");
			sh.Expr(this.args[i]);
		}
		//sh.Token(this.isMutable() ? "}" : "]");
		sh.Token("}");
	}

}
