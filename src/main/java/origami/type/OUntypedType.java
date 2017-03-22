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

package origami.type;

import origami.util.ODebug;

public class OUntypedType extends OTypeSystemType {

	OUntypedType(OTypeSystem ts) {
		super(ts);
	}

	@Override
	public Class<?> unwrap() {
		return Object.class;
	}

	@Override
	public String getLocalName() {
		return "?";
	}

	@Override
	public boolean isUntyped() {
		return true;
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		ODebug.NotAvailable(this);
		sb.append("Ljava.lang.Object;");
	}

}
