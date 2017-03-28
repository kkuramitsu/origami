// package origami.code;
//
// import java.util.Objects;
//
// import origami.asm.OrigamiBreakException;
// import origami.asm.OrigamiContinueException;
// import origami.lang.OEnv;
// import origami.lang.type.OType;
//
// public class OLabelBlockCode extends OParamCode<String> {
//
// public OLabelBlockCode(String label, OCode initCode, OCode body, OCode thus)
// {
// super(label, null, initCode, body, thus);
// }
//
// public OLabelBlockCode(OCode initCode, OCode body, OCode thus) {
// this(null, initCode, body, thus);
// }
//
// public String getLabel() {
// return this.getHandled();
// }
//
// public OCode initCode() {
// return this.getParams()[0];
// }
//
// public OCode bodyCode() {
// return this.getParams()[1];
// }
//
// public OCode thusCode() {
// return this.getParams()[2];
// }
//
// @Override
// public OType getType() {
// return this.thusCode().getType();
// }
//
// @Override
// public OCode refineType(OEnv env, OType t) {
// this.nodes[0] = this.thusCode().refineType(env, t);
// return this;
// }
//
// @Override
// public Object eval(OEnv env) throws Throwable {
// while (true) {
// try {
// this.bodyCode().eval(env);
// return this.thusCode();
// } catch (OrigamiContinueException e) {
//
// } catch (OrigamiBreakException e) {
// return this.thusCode();
// }
// }
// }
//
// @Override
// public void generate(OGenerator gen) {
//// gen.pushBlockCode(this);
// }
//
// static class OLabel {
// protected final OLabelBlockCode block;
//
// OLabel(OLabelBlockCode block) {
// this.block = block;
// }
//
// public boolean matchLabel(String name) {
// return Objects.equals(this.block.getLabel(), name) || name == null;
// }
//
// public OCode newHookCode(OEnv env, OCode expr) {
// return new OEmptyCode(env);
// }
//
// }
//
// public static class OBreakLabel extends OLabel {
//
// public OBreakLabel(OLabelBlockCode block) {
// super(block);
// }
//
// }
//
// public static class OContinueLabel extends OLabel {
//
// public OContinueLabel(OLabelBlockCode block) {
// super(block);
// }
//
// }
//
// }
