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

package blue.nez.parser.pasm;

import java.lang.reflect.Field;

import blue.origami.util.OStringUtils;

public abstract class PAsmInst extends PAsmAPI {
	// public int id;
	// public boolean joinPoint = false;
	// public final PAsmFunc apply;
	public PAsmInst next;

	public PAsmInst(PAsmInst next) {
		// this.id = -1;
		this.next = next;
	}

	public final String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		for (Field f : this.getClass().getDeclaredFields()) {
			sb.append(" ");
			sb.append(f.getName());
			sb.append("=");
			this.value(sb, f);
		}
		return sb.toString();
	}

	private void value(StringBuilder sb, Field f) {
		try {
			Object v = f.get(this);
			if (v instanceof boolean[]) {
				OStringUtils.formatHexicalByteSet(sb, (boolean[]) v);
			} else if (v instanceof byte[]) {
				OStringUtils.formatUTF8(sb, (byte[]) v);
			} else {
				sb.append(v);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			sb.append("?");
		}
	}

	public abstract PAsmInst exec(PAsmContext sc) throws PAsmTerminationException;

	private static PAsmInst[] emptyInst = new PAsmInst[0];

	public PAsmInst[] branch() {
		return emptyInst;
	}

}
