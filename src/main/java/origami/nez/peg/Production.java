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

import origami.util.StringCombinator;

public class Production implements StringCombinator {
	private final Grammar grammar;
	private final String name;
	private final Expression body;

	Production(Grammar grammar, String name, Expression body) {
		this.grammar = grammar;
		this.name = name;
		this.body = body;
	}

	public Grammar getGrammar() {
		return this.grammar;
	}

	public final String getLocalName() {
		return this.name;
	}

	public final String getUniqueName() {
		return grammar.getUniqueName(name);
	}

	public static String terminalName(String name) {
		return "\"" + name + "\"";
	}

	public final Expression getExpression() {
		return this.body;
	}

	private boolean isPublic = false;

	public final boolean isPublic() {
		return isPublic;
	}

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(name);
		sb.append(" = ");
		this.getExpression().strOut(sb);
	}

	// public static class Pname {
	// public final static String RootPrefix = "_";
	// public final static String TerminalPrefix = "\"";
	// public final static String PatternSuffix = "~";
	// public final String ns;
	// public final String name;
	// public final String suffix;
	// public final String lname;
	// public final String uname;
	//
	// public Pname(String ns, String name, String suffix) {
	// this.ns = ns;
	// this.name = name;
	// this.suffix = suffix;
	// this.lname = (suffix == null) ? name : name + suffix;
	// this.uname = ns + "." + lname;
	// }
	//
	// public final boolean isRoot() {
	// return this.ns.startsWith(RootPrefix);
	// }
	//
	// public boolean equalsNameSpace(String ns) {
	// return this.ns.equals(ns);
	// }
	//
	// public final boolean isTerminal() {
	// return name.startsWith(TerminalPrefix);
	// }
	//
	// public final String getLocalName() {
	// return this.lname;
	// }
	//
	// public final String getUniqueName() {
	// return this.uname;
	// }
	//
	// @Override
	// public final String toString() {
	// if (this.isRoot()) {
	// return this.lname;
	// } else {
	// return this.uname;
	// }
	// }
	//
	// public final static Pname parse(String ns, String name) {
	// int loc = name.lastIndexOf('.');
	// String lname = name;
	// String suffix = null;
	// if (loc > 0) {
	// ns = name.substring(0, loc);
	// lname = name.substring(loc + 1);
	// }
	// loc = lname.indexOf('&');
	// if (loc > 0) {
	// suffix = lname.substring(loc);
	// lname = lname.substring(0, loc);
	// }
	// return new Pname(ns, lname, suffix);
	// }
	//
	// public final static Pname newProductionName(String ns, String name,
	// HashMap<String, Pname> memo) {
	// if (memo != null) {
	// Pname pname = memo.get(name);
	// if (pname == null) {
	// pname = parse(ns, name);
	// memo.put(name, pname);
	// }
	// return pname;
	// }
	// return parse(ns, name);
	// }
	// }

}
