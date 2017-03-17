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

package origami.main;

import java.util.HashMap;

public class OOption {
	HashMap<String, Object> valueMap = new HashMap<>();

	public <T> void set(Class<T> c, T value) {
		valueMap.put(c.getName(), value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> c) {
		return (T) valueMap.get(c.getName());
	}

	public interface OPrototypeFactory<T> extends Cloneable {
		public Class<?> entryClass();

		public T clone();
	}

	public <T extends OPrototypeFactory<T>> T newInstance(Class<T> c) {
		return get(c).clone();
	}

	public void setFactory(String path) throws Throwable {
		Class<?> c = Class.forName(path);
		OPrototypeFactory<?> f = (OPrototypeFactory<?>) c.newInstance();
		valueMap.put(f.entryClass().getName(), f);
	}

}
