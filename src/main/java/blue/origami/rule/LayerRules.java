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

package blue.origami.rule;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import blue.origami.asm.OCallSite;
import blue.origami.asm.OClassLoader;
import blue.origami.code.NestedEnvCode;
import blue.origami.code.OCode;
import blue.origami.code.OErrorCode;
import blue.origami.ffi.OImportable;
import blue.origami.ffi.OrigamiException;
import blue.origami.lang.OEnv;
import blue.origami.lang.OGetter;
import blue.origami.lang.OGlobalVariable;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.OMethodWrapper;
import blue.origami.lang.OVariable;
import blue.origami.lang.OEnv.OBaseEnv;
import blue.origami.lang.OEnv.OListMatcher;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;
import blue.origami.nez.ast.Tree;
import blue.origami.util.ODebug;
import blue.origami.util.OTypeRule;

public class LayerRules implements OImportable, SyntaxAnalysis {

	public OTypeRule LayerDecl = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getText(_name, "");
			String ext = t.getText(_extends, null);
			OEnv caseEnv = null;
			if (ext != null) {
				OContextGroup base = env.get(ext, OContextGroup.class);
				if (base == null) {
					base = new OContextGroup(env, ext);
					env.add(t, ext, base);
				}
				caseEnv = base.newInnerEnv(name);
			} else {
				caseEnv = env.newEnv(name);
			}
			// addDefined(env, t, caseEnv);
			env.add(t, name, caseEnv);
			OCode mod = typeStmt(caseEnv, t.get(_body));
			return new NestedEnvCode(caseEnv, mod);
		}
	};

	public OTypeRule WithExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String[] names = parseNames(env, t.get(_name));
			// Arrays.reverse(names);
			OEnv top = env;
			for (String name : names) {
				ImportableEnv imported = env.get(name, ImportableEnv.class);
				// ODebug.trace("imported %s %s @%s", name, imported, env);
				if (imported == null) {
					return new OErrorCode(env, t, OFmt.undefined + " %s", name);
				}
				top = new LayeredEnv(top, imported);
			}
			top = top.newEnv(null); // adding new entry point;
			OCode c = typeExpr(top, t.get(_body));
			// ODebug.trace("with %s %s", c.getClass().getSimpleName(),
			// c.getType());
			return new NestedEnvCode(top, c);
		}
	};

	public OTypeRule WithoutExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String[] names = parseNames(env, t.get(_name));
			// Arrays.reverse(names);
			ArrayList<Rollback> rollbacks = new ArrayList<>();
			for (String name : names) {
				disableLayer(env, name, rollbacks);
			}
			OCode code = typeExpr(env, t.get(_body));
			for (Rollback r : rollbacks) {
				r.rollback();
			}
			return code;
		}
	};

	private void disableLayer(OEnv env, String name, ArrayList<Rollback> rollbacks) {
		for (OEnv cur = env; cur != null; cur = cur.getParent()) {
			if (cur instanceof LayeredEnv) {
				LayeredEnv imported = (LayeredEnv) cur;
				rollbacks.add(new Rollback(imported));
				imported.setEnabled(false);
			}
		}
	}

	static class Rollback {
		boolean memoed;
		LayeredEnv imported;

		public Rollback(LayeredEnv env) {
			this.imported = env;
			this.memoed = env.getEnabled();
		}

		void rollback() {
			imported.setEnabled(this.memoed);
		}
	}

	public final static void changeContext(OEnv env, String group, String context) {
		OContextGroup g = env.get(group, OContextGroup.class);
		if (g == null) {
			throw new OrigamiException("Context not found %s", group);
		}
		g.changeContext(context);
	}

	interface ImportableEnv extends OEnv {
		// boolean getEnabled();
		//
		// void setEnabled(boolean b);
	}

	static class OContextGroup extends OBaseEnv implements ImportableEnv {

		OContextGroup(OEnv parent, String name) {
			super(parent, name);
		}

		OEnv chosen = null;
		HashMap<String, OEnv> envMap = new HashMap<>();

		public OEnv newInnerEnv(String name) {
			OEnv layer = new OContextEnv(this, name);
			envMap.put(name, layer);
			if (chosen == null) {
				this.chosen = layer;
			}
			return layer;
		}

		public void addContextVariable(String name, OGlobalVariable gvar) {
			OContextVariable cvar = super.getLocal(name, false, OContextVariable.class);
			if (cvar == null) {
				ODebug.trace("adding context variable: %s on %s", name, this);
				cvar = new OContextVariable(this.getName(), gvar.isReadOnly(), gvar.getName(), gvar.getType());
				super.add(name, cvar);
			} else {
				if (!cvar.getType().eq(gvar.getType())) {
					throw new OErrorCode(this, OFmt.mismatched + " %s defined=%s", gvar.getType(), cvar.getType());
				}
			}
		}

		public void addContextVariable(String name, OMethodHandle mh) {
			OType[] p = mh.getThisParamTypes();
			OMethodHandle cmh = super.getLocal(name, false, OMethodHandle.class,
					(d, c) -> d.matchThisParams(p) ? d : null);
			if (cmh == null) {
				ODebug.trace("adding context method: %s on %s", name, this);
				cmh = new OContextMethod(this.getName(), mh);
				super.add(name, cmh);
			} else {
				if (!cmh.getReturnType().eq(mh.getReturnType())) {
					throw new OErrorCode(this, OFmt.mismatched + " %s defined=%s", mh, cmh);
				}
			}
		}

		@Override
		public OEnvEntry getDefined(String name, boolean isRuntime) {
			// ODebug.trace("%s isRuntime=%s", this.getName(), isRuntime);
			if (isRuntime) {
				return this.chosen.getDefined(name, isRuntime);
			}
			return super.getDefined(name, isRuntime);
		}

		ArrayList<ContextChangeListener> listenerList = new ArrayList<>();

		public void addContextListener(ContextChangeListener site) {
			listenerList.add(site);
		}

		public void changeContext(String name) {
			ODebug.trace("switching context %s => %s", chosen.getName(), name);
			OEnv e = this.envMap.get(name);
			if (e == null) {
				throw new OrigamiException("Context not found %s", name);
			}
			if (e != this.chosen) {
				this.chosen = e;
				for (ContextChangeListener l : listenerList) {
					l.changed();
				}
			}
		}

	}

	static class OContextEnv extends OBaseEnv implements ImportableEnv {
		private OContextGroup groupEnv;

		OContextEnv(OContextGroup parent, String name) {
			super(parent, name);
			this.groupEnv = parent;
		}

		@Override
		public void hookDefined(String name, OEnvEntry d) {
			Object v = d.getHandled();
			ODebug.trace("hooked %s handle=%s %s", this.getName(), v, v.getClass().getSimpleName());
			if (v instanceof OGlobalVariable) {
				groupEnv.addContextVariable(name, (OGlobalVariable) v);
			}
		}
	}

	static class LayeredEnv implements OEnv {

		private final OEnv parent;
		private final OEnv imported;

		LayeredEnv(OEnv parent, OEnv imported) {
			this.parent = parent;
			this.imported = imported;
		}

		@Override
		public String getName() {
			return "[" + this.imported.getName() + "]";
		}

		private boolean enabled = true;

		public boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(boolean b) {
			this.enabled = b;
		}

		@Override
		public OEnv getParent() {
			return this.parent;
		}

		@Override
		public Class<?> getEntryPoint() {
			return null;
		}

		@Override
		public OClassLoader getClassLoader() {
			return this.getParent().getClassLoader();
		}

		@Override
		public OTypeSystem getTypeSystem() {
			return this.getParent().getTypeSystem();
		}

		@Override
		public void addDefined(String name, OEnvEntry d) {
			ODebug.NotAvailable(this);
		}

		@Override
		public OEnvEntry getDefined(String name, boolean isRuntime) {
			if (enabled) {
				// ODebug.trace("finding %s isRuntime=%s %s on %s", name,
				// isRuntime, imported.getDefined(name, isRuntime),
				// this.getName());
				return imported.getDefined(name, isRuntime);
			}
			return null;
		}

	}

	private static OCallSite NameCallSite = new OContextVariableCallSite();
	private static OCallSite MethodCallSite = new OContextMethodCallSite();

	public static class OContextVariable extends OVariable {
		private final String contextGroupName;

		public OContextVariable(String groupName, boolean isReadOnly, String name, OType type) {
			super(isReadOnly, name, type);
			this.contextGroupName = groupName;
		}

		@Override
		public OCode nameCode(OEnv env, String name) {
			ODebug.FIXME(this);
			return null;
			// return new CallSiteCode(env, NameCallSite, this.getType(), name,
			// null, OCast.SAME, contextGroupName);
		}

		@Override
		public OCode defineCode(OEnv env, OCode right) {
			ODebug.NotAvailable(this);
			return null;
		}
	}

	public static class OContextMethod extends OMethodWrapper {
		private final String contextGroupName;

		public OContextMethod(String groupName, OMethodHandle mh) {
			super(mh);
			this.contextGroupName = groupName;
		}

		@Override
		public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
			ODebug.FIXME(this);
			return null;
			// MethodType mt = CallSiteCode.methodType(ret,
			// this.getThisParamTypes());
			// return new CallSiteCode(env, MethodCallSite, ret,
			// this.getLocalName(), mt, matchCost, contextGroupName);
		}

	}

	public interface ContextChangeListener {
		public void changed();
	}

	public abstract static class OContextCallSite extends OCallSite implements ContextChangeListener {
		OContextCallSite(OEnv env, String sig, String name, MethodType type) {
			super(env, name, sig, type);
		}

		private boolean changed = true;

		@Override
		public void changed() {
			this.changed = true;
		}

		@Override
		public boolean checkLocalGuard() {
			ODebug.trace("check guard");
			if (this.changed) {
				this.changed = false;
				return false;
			}
			return true;
		}

	}

	public static class OContextVariableCallSite extends OContextCallSite {
		public OContextVariableCallSite() {
			super(null, null, null, null);
		}

		public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, Class<?> entry,
				String context) throws Throwable {
			OEnv env = OEnv.resolve(entry);
			OContextGroup share = env.get(context, OContextGroup.class);
			OContextVariableCallSite site = new OContextVariableCallSite(env, name, OCallSite.Virtual, type);
			share.addContextListener(site);
			return site;
		}

		private OContextVariableCallSite(OEnv env, String name, String sig, MethodType type) {
			super(env, name, sig, type);
		}

		// @Override
		// public OCallSite newCallSite(OEnv env, String name, String sig,
		// MethodType methodType) {
		// return new OContextVariableCallSite(env, name, sig, methodType);
		// }

		@Override
		public void listMatchedMethods(OEnv env, OType base, String name, List<OMethodHandle> l,
				OListMatcher<OMethodHandle> mat) {
			OGlobalVariable g = env.get(name, OGlobalVariable.class);
			if (g != null) {
				l.add(new OGetter(g.getField()));
			}
		}
	}

	public static class OContextMethodCallSite extends OContextCallSite {
		public OContextMethodCallSite() {
			super(null, null, null, null);
		}

		public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, Class<?> entry,
				String context) throws Throwable {
			OEnv env = OEnv.resolve(entry);
			OContextGroup share = env.get(context, OContextGroup.class);
			OContextMethodCallSite site = new OContextMethodCallSite(env, name, OCallSite.Virtual, type);
			share.addContextListener(site);
			return site;
		}

		private OContextMethodCallSite(OEnv env, String name, String sig, MethodType type) {
			super(env, name, sig, type);
		}

		@Override
		public void listMatchedMethods(OEnv env, OType base, String name, List<OMethodHandle> l,
				OListMatcher<OMethodHandle> mat) {
			ODebug.TODO();
		}

	}

}
