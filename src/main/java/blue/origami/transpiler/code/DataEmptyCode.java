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

  @Override
	public Code asType(Env env, Ty ret) {
    if (ret.isVar()) {
      return this.asType(env, Ty.tData(((VarTy)ret).getName()));
    }else if (!(ret.isData())) {
      throw new ErrorCode(this, TFmt.type_error_YY1_YY2, ret, "Data");
    }
    this.setType(new DataTy());
    return super.castType(env, ret);
	}
}
