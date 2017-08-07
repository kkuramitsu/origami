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

package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Symbol;

public interface Symbols {
	public final static Symbol _name = Symbol.unique("name");

	public final static Symbol _value = Symbol.unique("value");

	public final static Symbol _body = Symbol.unique("body");
	public final static Symbol _type = Symbol.unique("type");
	public final static Symbol _expr = Symbol.unique("expr");
	public final static Symbol _list = Symbol.unique("list");
	public final static Symbol _param = Symbol.unique("param");

	public final static Symbol _suffix = Symbol.unique("suffix");

	public final static Symbol _base = Symbol.unique("base");
	public final static Symbol _cond = Symbol.unique("cond");
	public final static Symbol _msg = Symbol.unique("msg");
	public final static Symbol _then = Symbol.unique("then");
	public final static Symbol _else = Symbol.unique("else");
	public final static Symbol _init = Symbol.unique("init");
	public final static Symbol _iter = Symbol.unique("iter");
	public final static Symbol _label = Symbol.unique("label");
	public final static Symbol _try = Symbol.unique("try");
	public final static Symbol _catch = Symbol.unique("catch");
	public final static Symbol _finally = Symbol.unique("finally");
	public final static Symbol _left = Symbol.unique("left");
	public final static Symbol _right = Symbol.unique("right");
	public final static Symbol _recv = Symbol.unique("recv");
	public final static Symbol _size = Symbol.unique("size");
	public final static Symbol _prefix = Symbol.unique("prefix");
	public final static Symbol _start = Symbol.unique("start");
	public final static Symbol _end = Symbol.unique("end");

}
