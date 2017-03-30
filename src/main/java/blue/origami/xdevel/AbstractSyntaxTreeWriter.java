package blue.origami.xdevel;
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
// package origami.main.tool;
//
// import origami.main.CommonWriter;
// import origami.nez.ast.Symbol;
// import origami.nez.ast.Tree;
//
// import origami.trait.OStringUtils;
//
// public class AbstractSyntaxTreeWriter extends CommonWriter implements
/// TreeWriter {
//
// @Override
// public void writeTree(Tree<?> node) {
// writeAST(null, node);
// }
//
// private void writeAST(Symbol label, Tree<?> node) {
// if (node == null) {
// L("null");
// return;
// }
// if (label == null) {
// L("#" + node.getTag() + "[");
// } else {
// L("$" + label + ": #" + node.getTag() + "[");
// }
// if (node.size() == 0) {
// _L(OStringUtils.quoteString('\'', node.toText(), '\''));
// _L("]");
// } else {
// incIndent();
// for (int i = 0; i < node.size(); i++) {
// this.writeAST(node.getLabel(i), node.get(i));
// }
// decIndent();
// L("]");
// }
// }
// }
