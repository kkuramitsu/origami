package origami.xdevel;
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
// import origami.nez.ast.Tree;
//
// import origami.trait.OStringUtils;
//
// public class JSONTreeWriter extends CommonWriter implements TreeWriter {
//
// boolean dataOption = false;
//
// @Override
// public void writeTree(ParserFactory fac, Tree<?> node) {
// writeJSON(node);
// }
//
// private void writeJSON(Tree<?> node) {
// if (node.size() == 0) {
// String text = node.toText();
// if (dataOption) {
// try {
// Double v = Double.parseDouble(text);
// print(v.toString());
// return;
// } catch (NumberFormatException e) {
// }
// try {
// Long v = Long.parseLong(text);
// print(v.toString());
// return;
// } catch (NumberFormatException e) {
// }
// print(OStringUtils.quoteString('"', text, '"'));
// } else {
// print("{");
// print("\"type\":");
// print(OStringUtils.quoteString('"', node.getTag().toString(), '"'));
// print(",\"pos\":");
// long pos = node.getSourcePosition();
// print("" + pos);
// print(",\"line\":");
// print("" + node.getSource().linenum(pos));
// print(",\"column\":");
// print("" + node.getSource().column(pos));
// print(",\"text\":");
// print(OStringUtils.quoteString('"', text, '"'));
// print("}");
// }
// return;
// }
// if (node.isAllLabeled()) {
// print("{");
// if (!dataOption) {
// print("\"type\":");
// print(OStringUtils.quoteString('"', node.getTag().toString(), '"'));
// print(",");
// }
// for (int i = 0; i < node.size(); i++) {
// if (i > 0) {
// print(",");
// }
// print(OStringUtils.quoteString('"', node.getLabel(i).toString(), '"'));
// print(":");
// writeJSON(node.get(i));
// }
// print("}");
// return;
// }
// print("[");
// for (int i = 0; i < node.size(); i++) {
// if (i > 0) {
// print(",");
// }
// writeJSON(node.get(i));
// }
// print("]");
// }
//
// }
