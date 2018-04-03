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

public interface Symbols {
	public final static String _public = ("public");
	public final static String _name = ("name");

	public final static String _value = ("value");

	public final static String _body = ("body");
	public final static String _type = ("type");
	public final static String _expr = ("expr");
	public final static String _list = ("list");
	public final static String _param = ("param");

	public final static String _suffix = ("suffix");

	public final static String _base = ("base");
	public final static String _cond = ("cond");
	public final static String _where = ("where");
	public final static String _where2 = ("where2");

	public final static String _msg = ("msg");
	public final static String _then = ("then");
	public final static String _else = ("else");
	public final static String _init = ("init");
	public final static String _iter = ("iter");
	public final static String _label = ("label");
	public final static String _try = ("try");
	public final static String _catch = ("catch");
	public final static String _finally = ("finally");
	public final static String _left = ("left");
	public final static String _right = ("right");
	public final static String _recv = ("recv");
	public final static String _size = ("size");
	public final static String _prefix = ("prefix");
	public final static String _start = ("start");
	public final static String _end = ("end");
	public final static String _from = ("from");
	public final static String _to = ("to");

}
