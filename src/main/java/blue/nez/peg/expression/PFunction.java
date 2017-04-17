// package blue.nez.peg.expression;
//
// public interface PFunction {
//
// // @Override
// // public final boolean equals(Object o) {
// // if (o instanceof PFunction) {
// // PFunction f = (PFunction) o;
// // return this.funcName == f.funcName && this.param.equals(f.param) &&
// // this.get(0).equals(f.get(0));
// // }
// // return false;
// // }
//
// public default boolean hasInnerExpression() {
// return this.get(0) != defaultEmpty;
// }
//
// /* function */
// @Override
// public default void formatFunction(String name, StringBuilder sb) {
// sb.append("<");
// sb.append(this.funcName);
// if (this.param != null) {
// sb.append(" ");
// sb.append(this.param);
// }
// if (this.hasInnerExpression()) {
// sb.append(" ");
// sb.append(this.get(0));
// }
// sb.append(">");
// }
// }