/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package origami.nez.peg;

public class GrammarLoader {

	// public final static OGrammar load(String path, String[] paths) throws
	// IOException {
	// return load(CommonSource.newFileSource(path, paths);
	// }
	//
	// public final static OGrammar load(Source s) throws IOException {
	// OGrammar g = new OGrammar(s.getResourceName());
	// ExpressionParser ep = new ExpressionParser(null, g);
	// importFile(ep, g, s);
	// return g;
	// }
	//
	// public final static void importFile(ParserFactory factory,
	// ExpressionParser ep, OGrammar g, Source s) throws IOException {
	// Tree<?> t = OGrammar.NezParser.parse(s);
	// update(factory, ep, g, t);
	// }
	//
	// public final static void update(ParserFactory factory, ExpressionParser
	// ep, OGrammar g, Tree<?> node) throws IOException {
	// if (node.is(_Source)) {
	// for (Tree<?> sub : node) {
	// parse(factory, ep, g, sub);
	// }
	// }
	// }
	//
	// static void parse(ParserFactory factory, ExpressionParser ep, OGrammar g,
	// Tree<?> node) throws IOException {
	// if (node.is(_Production)) {
	// parseProduction(factory, g, ep, node);
	// return;
	// }
	// if (node.is(_Grammar)) {
	// }
	// if (node.is(_Import)) {
	// String name = node.getText(_name, null);
	// String path = name;
	// if (!name.startsWith("/") && !name.startsWith("\\")) {
	// path = extractFilePath(node.getSource().getResourceName()) + "/" + name;
	// }
	// importFile(factory, ep, g, CommonSource.newFileSource(path, null));
	// return;
	// }
	// }
	//
	// static void parseProduction(ParserFactory factory, OGrammar g,
	// ExpressionParser ep, Tree<?> node) {
	// Tree<?> nameNode = node.get(_name);
	// boolean isPublic = node.get(_public, null) != null;
	// String name = nameNode.toText();
	// if (nameNode.is(_String)) {
	// name = OProduction.terminalName(name);
	// }
	// Expression rule = g.getLocalExpression(name);
	// if (rule != null) {
	// factory.reportWarning(node, "duplicated production: " + name);
	// return;
	// }
	// g.addProduction(name, ep.newInstance(node.get(_expr)));
	// }

}
