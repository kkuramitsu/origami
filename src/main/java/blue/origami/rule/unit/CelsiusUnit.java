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

package blue.origami.rule.unit;

import blue.origami.ffi.OCast;

public class CelsiusUnit extends OUnit<CelsiusUnit> {

	public CelsiusUnit(double d) {
		super(d);
	}

	public CelsiusUnit() {
		super();
	}

	@Override
	public String unit() {
		return "C";
	}

	@Override
	public CelsiusUnit newValue(double d) {
		return new CelsiusUnit(d);
	}

	@OCast(cost = OCast.BXSAME)
	public final KelvinUnit toK() {
		return new KelvinUnit(this.doubleValue() + 273.15);
	}

	@OCast(cost = OCast.BXSAME)
	public final FahrenheitUnit toF() {
		return new FahrenheitUnit(this.doubleValue() * (9.0 / 5.0) + 32.0);
	}

}