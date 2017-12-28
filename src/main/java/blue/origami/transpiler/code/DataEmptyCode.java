package blue.origami.transpiler.code;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.VarTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.code.DataCode;

public class DataEmptyCode extends DataCode {
  public DataEmptyCode() {
    super(true, OArrays.emptyNames, OArrays.emptyCodes);
  }

  public Code cast(Env env, Ty ret) {
    if (ret.isVar()) {
      return cast(env, Ty.tData(((VarTy)ret).getName()));
    }else if (!(ret.isData())) {
      throw new ErrorCode(this, TFmt.type_error_YY1_YY2, ret, "Data");
    }
    DataTy dt = (DataTy) ret;
    this.names = dt.names();
    this.args = new Code[this.names.length];
    for (int i = 0; i < this.names.length; i++) {
      NameHint hint = env.findGlobalNameHint(env, names[i]);
      if (hint != null) {
        Code value = hint.getType().base().getDefaultValue();
        this.args[i] = value == null ? new DoneCode() : value;
        //ODebug.p("%s,%s,%s,%s",this.args[i],ret,value,hint.getType().base());
      } else {
        this.args[i] = new DoneCode();
        //ODebug.p("%s,%s",this.args[i],ret);
      }
    }
    this.setType(ret);
		return this;
  }

  @Override
	public Code asType(Env env, Ty ret) {
    if (ret.isVar()) {
      return this.asType(env, Ty.tData(((VarTy)ret).getName()));
    }else if (!(ret.isData())) {
      throw new ErrorCode(this, TFmt.type_error_YY1_YY2, ret, "Data");
    }
    this.setType(ret);
		return super.castType(env, ret);
	}
}
