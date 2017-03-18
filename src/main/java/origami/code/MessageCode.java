//package origami.code;
//
//import origami.OLog;
//import origami.nez.ast.Tree;
//import origami.trait.FormatMethods;
//import origami.type.OType;
//
//public abstract class MessageCode extends OParamCode<OLog> {
//	public final int level;
//
//	public MessageCode(int level, OLog m, OType ty, OCode... nodes) {
//		super(m, ty, nodes);
//		this.level = level;
//		m.setSourcePosition(this.getSourcePosition());
//	}
//
//	public MessageCode(int level, OLog m, OType ty) {
//		super(m, ty);
//		this.level = level;
//		m.setSourcePosition(this.getSourcePosition());
//	}
//
//
//	public String msg() {
//		return this.getLog().toString();
//	}
//
//}