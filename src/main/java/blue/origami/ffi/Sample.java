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

package blue.origami.ffi;

import blue.origami.rule.iroha.IObject;

@OAlias(name = "サンプル")
public class Sample extends IObject implements OrigamiObject {
	@OAlias(name = "名称")
	public String name;

	@OAlias(name = "重量")
	public double weight;

	public Sample(String name, double weight) {
		this.name = name;
		this.weight = weight;
	}

	public boolean isHeavy() {
		return weight > 10.0;
	}

	public String name() {
		return this.name;
	}

	@OMutable
	public void name(String name) {
		this.name = name;
	}

	@OCast(cost = OCast.CONV)
	public int toWeight() {
		return (int) this.weight;
	}

}
