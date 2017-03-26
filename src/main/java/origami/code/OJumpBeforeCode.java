/// ***********************************************************************
// * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// ***********************************************************************/
//
// package origami.code;
//
// import origami.asm.OBlock;
// import origami.lang.type.OUntypedType;
// import origami.util.ODebug;
//
// public class OJumpBeforeCode extends OParamCode<OCode[]> implements
/// OWrapperCode, OBlock {
//
// public OJumpBeforeCode(OCode expr) {
// super(new OCode[4], expr.getTypeSystem().newType(OUntypedType.class), expr);
// }
//
// @Override
// public OCode wrapped() {
// return this.getFirst();
// }
//
// @Override
// public void wrap(OCode code) {
// ODebug.NotAvailable(this);
// }
//
// public void setBeforeContinueCode(OCode code) {
// OCode[] codes = this.getHandled();
// codes[0] = code;
// }
//
// public void setBeforeBreakCode(OCode code) {
// OCode[] codes = this.getHandled();
// codes[1] = code;
// }
//
// public void setBeforeReturnCode(OCode code) {
// OCode[] codes = this.getHandled();
// codes[2] = code;
// }
//
// @Override
// public OCode getBeforeCode(OCode code) {
// OCode[] codes = this.getHandled();
// if (code instanceof OContinueCode) {
// return codes[0];
// }
// if (code instanceof OBreakCode) {
// return codes[1];
// }
// if (code instanceof OReturnCode) {
// return codes[2];
// }
// if (code instanceof OThrowCode) {
// return codes[3];
// }
// return null;
// }
//
// }